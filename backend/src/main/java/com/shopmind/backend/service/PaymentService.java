package com.shopmind.payment;

import com.shopmind.common.exception.BadRequestException;
import com.shopmind.common.exception.NotFoundException;
import com.shopmind.common.exception.UnauthorizedException;
import com.shopmind.order.OrderService;
import com.shopmind.order.OrderSummary;
import com.shopmind.payment.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final OrderService orders;
    private final PaymentProvider provider;
    private final String webhookSecret;
    private final boolean mockEnabled;

    public PaymentService(PaymentRepository payments, OrderService orders, PaymentProvider provider,
                          @Value("${app.payment.webhook-secret}") String webhookSecret,
                          @Value("${app.payment.mock-enabled}") boolean mockEnabled) {
        this.payments = payments;
        this.orders = orders;
        this.provider = provider;
        this.webhookSecret = webhookSecret;
        this.mockEnabled = mockEnabled;
    }

    /** Starts a payment for one of the user's pending orders. The amount comes from the order, never from the client. */
    @Transactional
    public PaymentResponse initiate(Long userId, Long orderId) {
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
            default -> throw new BadRequestException("Unsupported webhook status " + result);
        }
    }

    /** Development helper: the owner simulates the provider's answer. Disabled when mock-enabled=false. */
    @Transactional
    public PaymentResponse confirmMock(Long userId, Long paymentId, boolean success) {
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
