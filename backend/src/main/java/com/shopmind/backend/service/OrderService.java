package com.shopmind.order;

import com.shopmind.cart.CartService;
import com.shopmind.common.exception.BadRequestException;
import com.shopmind.common.exception.ConflictException;
import com.shopmind.common.exception.NotFoundException;
import com.shopmind.event.EventService;
import com.shopmind.event.EventType;
import com.shopmind.order.dto.OrderItemResponse;
import com.shopmind.order.dto.OrderResponse;
import com.shopmind.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    /** Reasons a customer may give when cancelling a not-yet-paid order. */
    private static final Set<CancelReason> CUSTOMER_REASONS = Set.of(CancelReason.CHANGED_MIND, CancelReason.PAYMENT_FAILED);

    private final OrderRepository orders;
    private final CartService cart;
    private final ProductService products;
    private final EventService events;

    public OrderService(OrderRepository orders, CartService cart, ProductService products, EventService events) {
        this.orders = orders;
        this.cart = cart;
        this.products = products;
        this.events = events;
    }

    // ---------------- customer ----------------

    /** Cart -> order. Stock is reduced under row locks in the same transaction. */
    @Transactional
    public OrderResponse checkout(Long userId) {
        List<CartService.CartLine> lines = cart.checkoutLines(userId).stream()
                .sorted(Comparator.comparing((CartService.CartLine l) -> l.product().getId()))  // stable lock order
                .toList();
        if (lines.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        ShopOrder order = new ShopOrder();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        BigDecimal total = BigDecimal.ZERO;
        for (CartService.CartLine line : lines) {
            products.decreaseStock(line.product().getId(), line.quantity());
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(line.product());
            item.setQuantity(line.quantity());
            item.setUnitPrice(line.product().getPrice());
            order.getItems().add(item);
            total = total.add(line.product().getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        order.setTotal(total);
        orders.save(order);
        cart.clear(userId);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return orders.findByUserId(userId, PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return toResponse(orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found")));
    }

    /** Customers can cancel only orders that are not paid yet. After payment, use a refund. */
    @Transactional
    public OrderResponse cancelByCustomer(Long userId, Long orderId, CancelReason reason) {
        ShopOrder order = lock(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException("Order " + orderId + " not found");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Only pending orders can be cancelled. Paid orders need a refund request.");
        }
        CancelReason effective = reason == null ? CancelReason.CHANGED_MIND : reason;
        if (!CUSTOMER_REASONS.contains(effective)) {
            throw new BadRequestException("Reason must be one of " + CUSTOMER_REASONS);
        }
        applyStatus(order, OrderStatus.CANCELLED, effective);
        return toResponse(order);
    }

    // ---------------- admin ----------------

    /** SHIPPED, DELIVERED, REFUNDED, RETURNED, or CANCELLED. PAID only comes from the payment module. */
    @Transactional
    public OrderResponse adminUpdateStatus(Long orderId, OrderStatus next, CancelReason reason) {
        if (next == OrderStatus.PAID || next == OrderStatus.PENDING) {
            throw new BadRequestException("Status " + next + " cannot be set manually");
        }
        if (next.requiresReason() && reason == null) {
            throw new BadRequestException("A reason is required for status " + next);
        }
        ShopOrder order = lock(orderId);
        applyStatus(order, next, reason);
        return toResponse(order);
    }

    // ---------------- used by the payment module ----------------

    @Transactional(readOnly = true)
    public OrderSummary getPayable(Long userId, Long orderId) {
        ShopOrder order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order " + orderId + " is " + order.getStatus() + " and cannot be paid");
        }
        return new OrderSummary(order.getId(), order.getTotal(), order.getStatus());
    }

    /** Idempotent: calling it again for an already paid order does nothing. */
    @Transactional
    public void markPaid(Long orderId) {
        ShopOrder order = lock(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            applyStatus(order, OrderStatus.PAID, null);
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            // Money arrived for a cancelled order: needs a manual refund. Surface it, do not hide it.
            throw new ConflictException("Order " + orderId + " was cancelled but a payment succeeded. Refund required.");
        }
    }

    // ---------------- internals ----------------

    private ShopOrder lock(Long orderId) {
        return orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
    }

    private void applyStatus(ShopOrder order, OrderStatus next, CancelReason reason) {
        if (!order.getStatus().canTransitionTo(next)) {
            throw new ConflictException("Cannot change order " + order.getId() + " from " + order.getStatus() + " to " + next);
        }
        order.setStatus(next);
        order.setCancelReason(next.requiresReason() ? reason : null);

        if (next == OrderStatus.CANCELLED) {
            order.getItems().stream()
                    .sorted(Comparator.comparing((OrderItem i) -> i.getProduct().getId()))
                    .forEach(i -> products.increaseStock(i.getProduct().getId(), i.getQuantity()));
        }

        EventType eventType = switch (next) {
            case PAID -> EventType.PURCHASE;
            case CANCELLED -> EventType.ORDER_CANCELLED;
            case REFUNDED -> EventType.ORDER_REFUNDED;
            case RETURNED -> EventType.ORDER_RETURNED;
            default -> null;
        };
        if (eventType != null) {
            for (OrderItem item : order.getItems()) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("orderId", order.getId());
                meta.put("quantity", item.getQuantity());
                if (reason != null) meta.put("reason", reason.name());
                events.record(order.getUserId(), item.getProduct().getId(), eventType, meta);
            }
        }
    }

    private OrderResponse toResponse(ShopOrder o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(o.getId(), o.getStatus(), o.getCancelReason(), o.getTotal(), o.getCreatedAt(), items);
    }
}
