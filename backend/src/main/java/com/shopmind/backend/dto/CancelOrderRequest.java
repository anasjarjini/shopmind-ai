package com.shopmind.backend.dto;

import com.shopmind.backend.entity.CancelReason;

/** reason is optional for the customer (defaults to CHANGED_MIND). */
public record CancelOrderRequest(CancelReason reason) {}
