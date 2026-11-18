package com.shopmind.backend.service;

import java.math.BigDecimal;
import java.util.UUID;

/** Add a StripePaymentProvider later without touching PaymentService. */
public interface PaymentProvider {

    record Intent(String providerRef, String checkoutUrl) {}

    String name();

    Intent createIntent(UUID orderId, BigDecimal amount);
}
