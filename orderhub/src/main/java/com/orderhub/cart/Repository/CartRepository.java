package com.orderhub.cart.Repository;

import com.orderhub.cart.Cart;
import com.orderhub.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Long> {

     @EntityGraph(attributePaths = {"items", "items.product"})
     Optional<Cart> findByUserId(Long userId);

     Optional<Cart>findById(Long Id);

}
