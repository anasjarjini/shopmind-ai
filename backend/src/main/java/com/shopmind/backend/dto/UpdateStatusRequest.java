package com.shopmind.order.dto;

import com.shopmind.order.CancelReason;
import com.shopmind.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status, CancelReason reason) {}
