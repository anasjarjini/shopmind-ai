package com.shopmind.order;

import com.shopmind.auth.AuthPrincipal;
import com.shopmind.order.dto.CancelOrderRequest;
import com.shopmind.order.dto.OrderResponse;
import com.shopmind.order.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Creates an order from the current cart. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal AuthPrincipal p) {
        return service.checkout(p.id());
    }

    @GetMapping
    public Page<OrderResponse> list(@AuthenticationPrincipal AuthPrincipal p,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return service.list(p.id(), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long id) {
        return service.get(p.id(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long id,
                                @RequestBody(required = false) CancelOrderRequest req) {
        return service.cancelByCustomer(p.id(), id, req == null ? null : req.reason());
    }

    /** ADMIN only (see SecurityConfig). */
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return service.adminUpdateStatus(id, req.status(), req.reason());
    }
}
