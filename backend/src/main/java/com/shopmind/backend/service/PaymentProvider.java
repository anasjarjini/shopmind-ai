package com.shopmind.payment;

import java.math.BigDecimal;

/** Add a StripePaymentProvider later without touching PaymentService. */
public interface PaymentProvider {

    record Intent(String providerRef, String checkoutUrl) {}

    String name();

    Intent createIntent(Long orderId, BigDecimal amount);
}
