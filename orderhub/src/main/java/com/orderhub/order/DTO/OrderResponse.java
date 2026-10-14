package com.orderhub.order.DTO;

import com.orderhub.order.OrderStatus;
import com.orderhub.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount,
                            LocalDateTime expiresAt, LocalDateTime createdAt,
                            List<OrderItemResponse> items) {

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getExpiresAt(),
                o.getCreatedAt(), o.getItems().stream().map(OrderItemResponse::from).toList());
    }

}
