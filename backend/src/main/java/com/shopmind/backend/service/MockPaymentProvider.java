package com.shopmind.backend.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public Intent createIntent(UUID orderId, BigDecimal amount) {
        return new Intent("mock_" + UUID.randomUUID(), null);
    }
}
