package com.orderhub.cart.DTO;

import com.orderhub.cart.Cart;
import com.orderhub.user.User;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long cartId, List<CartItemResponse> items, BigDecimal total) {

        public static CartResponse from(Cart cart) {
                List<CartItemResponse> items = cart.getItems().stream()
                        .map(CartItemResponse::from)
                        .toList();
                BigDecimal total = items.stream()
                        .map(CartItemResponse::lineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new CartResponse(cart.getId(), items, total);
        }
}
