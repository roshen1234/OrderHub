package com.orderhub.payment.DTO;

import com.orderhub.payment.PaymentStatus;
import com.orderhub.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, Long orderId, BigDecimal amount,
                              PaymentStatus status, String providerRef, LocalDateTime createdAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrder().getId(), p.getAmount(),
                p.getStatus(), p.getProviderRef(), p.getCreatedAt());
    }
}
