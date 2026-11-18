package com.shopmind.backend.dto;

import com.shopmind.backend.entity.CancelReason;
import com.shopmind.backend.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, OrderStatus status, CancelReason cancelReason, BigDecimal total,
                            Instant createdAt, List<OrderItemResponse> items) {}
