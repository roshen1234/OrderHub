package com.orderhub.cart.Repository;

import com.orderhub.cart.Cart;
import com.orderhub.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Long> {

     @EntityGraph(attributePaths = {"items", "items.product"})
     Optional<Cart> findByUserId(Long userId);

     Optional<Cart>findById(Long Id);

     // locks the cart do that when once person checkout then he cant click again checkout it is locked only once the transaction complete then he can click again
     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
     Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);

}
