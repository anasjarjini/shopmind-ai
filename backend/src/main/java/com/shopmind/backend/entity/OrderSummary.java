package com.shopmind.order;

import java.math.BigDecimal;

/** Small read model the payment module uses, so it never touches order entities. */
public record OrderSummary(Long id, BigDecimal total, OrderStatus status) {}
