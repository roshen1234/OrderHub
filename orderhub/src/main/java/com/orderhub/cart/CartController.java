package com.orderhub.cart;

import com.orderhub.cart.DTO.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    public final CartService cartService;

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
         return cartService.getCart(authentication.getName());   // the email

    }

}
