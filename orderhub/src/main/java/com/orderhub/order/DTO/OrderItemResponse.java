package com.orderhub.order.DTO;

import com.orderhub.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, int quantity,
                                BigDecimal unitPrice, BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem i) {
        return new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(),
                i.getQuantity(), i.getUnitPrice(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));

    }
}
