package com.orderhub.cart.Repository;

import com.orderhub.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // the ownership check is built into the query: the item must belong to this user's cart
    Optional<CartItem> findByIdAndCartUserEmail(Long id, String email);

}
