package com.orderhub.cart.DTO;

import com.orderhub.cart.Cart;
import com.orderhub.cart.CartItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CartItemResponse(Long id, Long productId, String productName,
                               BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    public static CartItemResponse from(CartItem i) {

        BigDecimal price = i.getProduct().getPrice();

        return new CartItemResponse(i.getId(), i.getProduct().getId(), i.getProduct().getName(),
                price, i.getQuantity(), price.multiply(BigDecimal.valueOf(i.getQuantity())));
    }
}
