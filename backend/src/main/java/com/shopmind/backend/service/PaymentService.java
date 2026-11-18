package com.shopmind.backend.service;

import com.shopmind.backend.dto.PaymentResponse;
import com.shopmind.backend.entity.OrderSummary;
import com.shopmind.backend.entity.Payment;
import com.shopmind.backend.entity.PaymentStatus;
import com.shopmind.backend.exception.NotFoundException;
import com.shopmind.backend.exception.UnauthorizedException;
import com.shopmind.backend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private OrderService orders;

    @Autowired
    private PaymentProvider provider;

    @Value("${payment.webhook-secret:${app.payment.webhook-secret:shopmind-webhook-secret-dev}}")
    private String webhookSecret;

    @Value("${payment.mock-enabled:${app.payment.mock-enabled:true}}")
    private boolean mockEnabled;

    /** Starts a payment for one of the user's pending orders. The amount comes from the order, never from the client. */
    @Transactional
    public PaymentResponse initiate(UUID userId, UUID orderId) {
        OrderSummary order = orders.getPayable(userId, orderId);
        PaymentProvider.Intent intent = provider.createIntent(order.id(), order.total());

        Payment p = new Payment();
        p.setOrderId(order.id());
        p.setProvider(provider.name());
        p.setProviderRef(intent.providerRef());
        p.setAmount(order.total());
        p.setStatus(PaymentStatus.INITIATED);
        payments.save(p);
        return PaymentResponse.from(p, intent.checkoutUrl());
    }

    /** Called by the provider webhook. Idempotent: a payment that already left INITIATED is ignored. */
    @Transactional
    public void handleProviderEvent(String providerRef, PaymentStatus result) {
        Payment p = payments.findWithLockByProviderRef(providerRef)
                .orElseThrow(() -> new NotFoundException("Unknown payment reference"));
        if (p.getStatus() != PaymentStatus.INITIATED) {
            return;
        }
        switch (result) {
            case SUCCEEDED -> {
                p.setStatus(PaymentStatus.SUCCEEDED);
                orders.markPaid(p.getOrderId());
            }
            case FAILED -> p.setStatus(PaymentStatus.FAILED);
            default -> throw new IllegalArgumentException("Unsupported webhook status " + result);
        }
    }

    @Transactional
    public PaymentResponse confirmMock(UUID userId, UUID paymentId, boolean success) {
        if (!mockEnabled) {
            throw new NotFoundException("Not found");
        }
        Payment p = payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment " + paymentId + " not found"));
        orders.getPayable(userId, p.getOrderId());   // ownership + still pending
        handleProviderEvent(p.getProviderRef(), success ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
        return PaymentResponse.from(payments.findById(paymentId).orElseThrow(), null);
    }

    public void verifyWebhookSecret(String header) {
        boolean ok = header != null && MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8), webhookSecret.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw new UnauthorizedException("Invalid webhook secret");
        }
    }
}
