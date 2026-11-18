package com.shopmind.backend.controller;

import com.shopmind.backend.dto.CreatePaymentRequest;
import com.shopmind.backend.dto.PaymentResponse;
import com.shopmind.backend.dto.WebhookRequest;
import com.shopmind.backend.entity.User;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@AuthenticationPrincipal User user,
                                  @Valid @RequestBody CreatePaymentRequest req) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.initiate(user.getId(), req.orderId());
    }

    /** Public URL, protected by the X-Webhook-Secret header. */
    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void webhook(@RequestHeader(name = "X-Webhook-Secret", required = false) String secret,
                        @Valid @RequestBody WebhookRequest req) {
        service.verifyWebhookSecret(secret);
        service.handleProviderEvent(req.providerRef(), req.status());
    }

    /** Dev only: simulate the provider (success=true or false). */
    @PostMapping("/{id}/mock-confirm")
    public PaymentResponse mockConfirm(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                       @RequestParam(defaultValue = "true") boolean success) {
        if (user == null) {
            throw new UnauthorizedException("Non authentifié");
        }
        return service.confirmMock(user.getId(), id, success);
    }
}
