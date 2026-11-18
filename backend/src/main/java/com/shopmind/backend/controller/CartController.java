package com.shopmind.backend.controller;

import com.shopmind.backend.dto.AddCartItemRequest;
import com.shopmind.backend.dto.CartResponse;
import com.shopmind.backend.dto.UpdateCartItemRequest;
import com.shopmind.backend.entity.User;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService service;

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.get(user.getId());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(@AuthenticationPrincipal User user, @Valid @RequestBody AddCartItemRequest req) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.addItem(user.getId(), req.productId(), req.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal User user, @PathVariable Long productId,
                               @Valid @RequestBody UpdateCartItemRequest req) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.setQuantity(user.getId(), productId, req.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal User user, @PathVariable Long productId) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.removeItem(user.getId(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        service.clear(user.getId());
    }
}
