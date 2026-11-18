package com.shopmind.payment.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(@NotNull Long orderId) {}
