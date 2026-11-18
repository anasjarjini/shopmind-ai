package com.shopmind.backend.service;

import com.shopmind.backend.dto.OrderItemResponse;
import com.shopmind.backend.dto.OrderResponse;
import com.shopmind.backend.entity.*;
import com.shopmind.backend.exception.ConflictException;
import com.shopmind.backend.exception.NotFoundException;
import com.shopmind.backend.repository.OrderRepository;
import com.shopmind.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    /** Reasons a customer may give when cancelling a not-yet-paid order. */
    private static final Set<CancelReason> CUSTOMER_REASONS = Set.of(CancelReason.CHANGED_MIND, CancelReason.PAYMENT_FAILED);

    @Autowired
    private OrderRepository orders;

    @Autowired
    private CartService cart;

    @Autowired
    private ProductRepository productRepository;

    // ---------------- customer ----------------


    @Transactional
    public OrderResponse checkout(UUID userId) {
        List<CartService.CartLine> lines = cart.checkoutLines(userId).stream()
                .sorted(Comparator.comparing((CartService.CartLine l) -> l.product().getId()))  // stable lock order
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        ShopOrder order = new ShopOrder();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        BigDecimal total = BigDecimal.ZERO;
        for (CartService.CartLine line : lines) {
            decreaseStock(line.product(), line.quantity());
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
    public Page<OrderResponse> list(UUID userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return orders.findByUserId(userId, PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID userId, UUID orderId) {
        return toResponse(orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found")));
    }

    /** Customers can cancel only orders that are not paid yet. After payment, use a refund. */
    @Transactional
    public OrderResponse cancelByCustomer(UUID userId, UUID orderId, CancelReason reason) {
        ShopOrder order = lock(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException("Order " + orderId + " not found");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Only pending orders can be cancelled. Paid orders need a refund request.");
        }
        CancelReason effective = reason == null ? CancelReason.CHANGED_MIND : reason;
        if (!CUSTOMER_REASONS.contains(effective)) {
            throw new IllegalArgumentException("Reason must be one of " + CUSTOMER_REASONS);
        }
        applyStatus(order, OrderStatus.CANCELLED, effective);
        return toResponse(order);
    }




    @Transactional
    public OrderResponse adminUpdateStatus(UUID orderId, OrderStatus next, CancelReason reason) {
        if (next == OrderStatus.PAID || next == OrderStatus.PENDING) {
            throw new IllegalArgumentException("Status " + next + " cannot be set manually");
        }
        if (next.requiresReason() && reason == null) {
            throw new IllegalArgumentException("A reason is required for status " + next);
        }
        ShopOrder order = lock(orderId);
        applyStatus(order, next, reason);
        return toResponse(order);
    }



    @Transactional(readOnly = true)
    public OrderSummary getPayable(UUID userId, UUID orderId) {
        ShopOrder order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order " + orderId + " is " + order.getStatus() + " and cannot be paid");
        }
        return new OrderSummary(order.getId(), order.getTotal(), order.getStatus());
    }

    /** Idempotent: calling it again for an already paid order does nothing. */
    @Transactional
    public void markPaid(UUID orderId) {
        ShopOrder order = lock(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            applyStatus(order, OrderStatus.PAID, null);
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            // Money arrived for a cancelled order: needs a manual refund. Surface it, do not hide it.
            throw new ConflictException("Order " + orderId + " was cancelled but a payment succeeded. Refund required.");
        }
    }



    private ShopOrder lock(UUID orderId) {
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
                    .forEach(i -> increaseStock(i.getProduct(), i.getQuantity()));
        }
    }

    private void decreaseStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new ConflictException("Insufficient stock for " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    private void increaseStock(Product product, int quantity) {
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }

    private OrderResponse toResponse(ShopOrder o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(o.getId(), o.getStatus(), o.getCancelReason(), o.getTotal(), o.getCreatedAt(), items);
    }
}
