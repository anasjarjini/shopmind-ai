package com.shopmind.backend.dto;

import com.shopmind.backend.entity.Payment;
import com.shopmind.backend.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, String provider, String providerRef,
                              BigDecimal amount, PaymentStatus status, String checkoutUrl) {

    public static PaymentResponse from(Payment p, String checkoutUrl) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getProvider(), p.getProviderRef(),
                p.getAmount(), p.getStatus(), checkoutUrl);
    }
}
