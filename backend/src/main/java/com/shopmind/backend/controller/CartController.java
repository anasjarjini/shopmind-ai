package com.shopmind.cart;

import com.shopmind.auth.AuthPrincipal;
import com.shopmind.cart.dto.AddCartItemRequest;
import com.shopmind.cart.dto.CartResponse;
import com.shopmind.cart.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthPrincipal p) {
        return service.get(p.id());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody AddCartItemRequest req) {
        return service.addItem(p.id(), req.productId(), req.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long productId,
                               @Valid @RequestBody UpdateCartItemRequest req) {
        return service.setQuantity(p.id(), productId, req.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long productId) {
        return service.removeItem(p.id(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AuthPrincipal p) {
        service.clear(p.id());
    }
}
