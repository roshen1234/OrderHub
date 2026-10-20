package com.orderhub.order.DTO;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "addressId is required") Long addressId
) {}