package com.shopmind.payment;

import com.shopmind.auth.AuthPrincipal;
import com.shopmind.payment.dto.CreatePaymentRequest;
import com.shopmind.payment.dto.PaymentResponse;
import com.shopmind.payment.dto.WebhookRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@AuthenticationPrincipal AuthPrincipal p,
                                  @Valid @RequestBody CreatePaymentRequest req) {
        return service.initiate(p.id(), req.orderId());
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
    public PaymentResponse mockConfirm(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long id,
                                       @RequestParam(defaultValue = "true") boolean success) {
        return service.confirmMock(p.id(), id, success);
    }
}
