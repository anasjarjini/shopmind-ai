package com.shopmind.payment.dto;

import com.shopmind.payment.Payment;
import com.shopmind.payment.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(Long id, Long orderId, String provider, String providerRef,
                              BigDecimal amount, PaymentStatus status, String checkoutUrl) {

    public static PaymentResponse from(Payment p, String checkoutUrl) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getProvider(), p.getProviderRef(),
                p.getAmount(), p.getStatus(), checkoutUrl);
    }
}
