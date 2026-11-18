package com.shopmind.payment.dto;

import com.shopmind.payment.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookRequest(@NotBlank String providerRef, @NotNull PaymentStatus status) {}
