package com.orderhub.order.repository;

import com.orderhub.order.OrderStatus;
import com.orderhub.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Long> {
    // OrderRepository
    @Query("SELECT o.id FROM Order o WHERE o.status = :status AND o.expiresAt < :now")
    List<Long> findExpiredIds(@Param("status") OrderStatus status, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Order o SET o.status = :next WHERE o.id = :id AND o.status = :expected")
    int transition(@Param("id") Long id, @Param("expected") OrderStatus expected, @Param("next") OrderStatus next);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsById(Long id);


    Page<Order> findByUserEmail(String email, Pageable pageable);

    @Query(
           "    FROM Order o" +
           "    JOIN FETCH o.items" +
           "    JOIN FETCH o.items.product" +
           "    WHERE o.user.id = :userId"+
           "    AND o.id= :orderId"
    )
    Optional<Order> findAllByUserIdAndId(@Param("userId") Long userId,@Param("orderId") Long orderId);

}