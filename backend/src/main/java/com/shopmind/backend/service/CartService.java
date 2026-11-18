package com.shopmind.backend.service;

import com.shopmind.backend.dto.CartItemResponse;
import com.shopmind.backend.dto.CartResponse;
import com.shopmind.backend.entity.Cart;
import com.shopmind.backend.entity.CartItem;
import com.shopmind.backend.entity.Product;
import com.shopmind.backend.exception.ConflictException;
import com.shopmind.backend.exception.NotFoundException;
import com.shopmind.backend.repository.CartRepository;
import com.shopmind.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    /** What the order module needs to turn a cart into an order. */
    public record CartLine(Product product, int quantity) {}

    @Autowired
    private CartRepository carts;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public CartResponse get(UUID userId) {
        return toResponse(getOrCreate(userId));
    }

    @Transactional
    public CartResponse addItem(UUID userId, Long productId, int quantity) {
        Cart cart = getOrCreate(userId);
        Product product = getActiveProduct(productId);
        Optional<CartItem> existing = findItem(cart, productId);
        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + quantity;
        checkStock(product, newQuantity);

        if (existing.isPresent()) {
            existing.get().setQuantity(newQuantity);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(newQuantity);
            cart.getItems().add(item);
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse setQuantity(UUID userId, Long productId, int quantity) {
        Cart cart = getOrCreate(userId);
        CartItem item = findItem(cart, productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " is not in the cart"));
        if (quantity == 0) {
            cart.getItems().remove(item);
        } else {
            checkStock(item.getProduct(), quantity);
            item.setQuantity(quantity);
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, Long productId) {
        Cart cart = getOrCreate(userId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toResponse(cart);
    }

    @Transactional
    public void clear(UUID userId) {
        carts.findByUserId(userId).ifPresent(c -> c.getItems().clear());
    }

    @Transactional(readOnly = true)
    public List<CartLine> checkoutLines(UUID userId) {
        return carts.findByUserId(userId)
                .map(c -> c.getItems().stream().map(i -> new CartLine(i.getProduct(), i.getQuantity())).toList())
                .orElse(List.of());
    }

    // ---------------- internals ----------------

    private Cart getOrCreate(UUID userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return carts.save(c);
        });
    }

    private Optional<CartItem> findItem(Cart cart, Long productId) {
        return cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst();
    }

    private Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ConflictException("Product is not active: " + productId);
        }
        return product;
    }

    private void checkStock(Product product, int wanted) {
        if (product.getStock() < wanted) {
            throw new ConflictException("Only " + product.getStock() + " in stock for '" + product.getName() + "'");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CartItemResponse(p.getId(), p.getName(), p.getImageUrl(), p.getPrice(), i.getQuantity(), subtotal);
        }).toList();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, total);
    }
}

