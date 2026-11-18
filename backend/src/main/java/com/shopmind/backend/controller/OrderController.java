package com.shopmind.backend.controller;

import com.shopmind.backend.dto.CancelOrderRequest;
import com.shopmind.backend.dto.OrderResponse;
import com.shopmind.backend.dto.UpdateStatusRequest;
import com.shopmind.backend.entity.User;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService service;

    /** Creates an order from the current cart. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.checkout(user.getId());
    }

    @GetMapping
    public Page<OrderResponse> list(@AuthenticationPrincipal User user,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.list(user.getId(), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.get(user.getId(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                @RequestBody(required = false) CancelOrderRequest req) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.cancelByCustomer(user.getId(), id, req == null ? null : req.reason());
    }

    /** ADMIN only (see SecurityConfig). */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest req) {
        return service.adminUpdateStatus(id, req.status(), req.reason());
    }
}
