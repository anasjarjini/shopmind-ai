package com.shopmind.backend.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** Small read model the payment module uses, so it never touches order entities. */
public record OrderSummary(UUID id, BigDecimal total, OrderStatus status) {}
