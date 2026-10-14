package com.orderhub.order.DTO;

import com.orderhub.order.OrderStatus;
import com.orderhub.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long id, OrderStatus status, BigDecimal totalAmount,
                                   LocalDateTime createdAt, LocalDateTime expiresAt) {
    public static OrderSummaryResponse from(Order o) {
        return new OrderSummaryResponse(o.getId(), o.getStatus(), o.getTotalAmount(),
                o.getCreatedAt(), o.getExpiresAt());
    }
}
