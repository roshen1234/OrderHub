package com.orderhub.inventory;

import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(@NotNull(message = "delta is required") Integer delta) {}
