package com.orderhub.payment;

import com.orderhub.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUserEmailAndIdempotencyKey(String email, String idempotencyKey);

    Optional<Payment> findByProviderRef(String providerRef);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :next WHERE p.id = :id AND p.status = :expected")
    int transition(@Param("id") Long id, @Param("expected") PaymentStatus expected,
                   @Param("next") PaymentStatus next);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :next WHERE p.order.id = :orderId AND p.status = :expected")
    int transitionByOrderId(@Param("orderId") Long orderId, @Param("expected") PaymentStatus expected,
                            @Param("next") PaymentStatus next);

    @Query("SELECT p.id FROM Payment p WHERE p.status = :status")
    List<Long> findIdsByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT p.id FROM Payment p WHERE p.status = :status AND p.createdAt < :before")
    List<Long> findIdsByStatusAndCreatedBefore(@Param("status") PaymentStatus status,
                                               @Param("before") LocalDateTime before);
}
