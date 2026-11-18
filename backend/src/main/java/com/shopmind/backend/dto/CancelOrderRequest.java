package com.shopmind.order.dto;

import com.shopmind.order.CancelReason;

/** reason is optional for the customer (defaults to CHANGED_MIND). */
public record CancelOrderRequest(CancelReason reason) {}
