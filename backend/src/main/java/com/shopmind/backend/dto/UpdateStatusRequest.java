package com.shopmind.backend.dto;

import com.shopmind.backend.entity.CancelReason;
import com.shopmind.backend.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status, CancelReason reason) {}
