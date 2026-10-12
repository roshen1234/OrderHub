package com.orderhub.cart;

import com.orderhub.cart.DTO.CartResponse;
import com.orderhub.cart.Repository.CartRepository;
import com.orderhub.user.User;
import com.orderhub.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {


    public final CartRepository cartRepository;
    public final UserRepository userRepository;

    @Transactional
    Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUser(user);
                    return cartRepository.save(c);
                });
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        return CartResponse.from(getOrCreateCart(email));   // conversion inside the transaction
    }

}
