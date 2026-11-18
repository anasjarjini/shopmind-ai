package com.shopmind.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** quantity 0 removes the item. */
public record UpdateCartItemRequest(@NotNull @Min(0) @Max(99) Integer quantity) {}
