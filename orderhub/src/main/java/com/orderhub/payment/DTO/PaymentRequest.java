package com.orderhub.payment.DTO;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "orderid is needed")
        Long orderId

) {
}
