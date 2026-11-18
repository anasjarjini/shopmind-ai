package com.shopmind.order.dto;

import com.shopmind.order.CancelReason;
import com.shopmind.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, CancelReason cancelReason, BigDecimal total,
                            Instant createdAt, List<OrderItemResponse> items) {}
