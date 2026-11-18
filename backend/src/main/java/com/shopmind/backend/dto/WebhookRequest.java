package com.shopmind.backend.dto;

import com.shopmind.backend.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookRequest(@NotBlank String providerRef, @NotNull PaymentStatus status) {}
