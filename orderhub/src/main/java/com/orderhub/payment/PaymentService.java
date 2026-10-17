package com.orderhub.payment;

import com.orderhub.common.exception.ConflictException;
import com.orderhub.common.exception.GatewayUnavailableException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.order.OrderStatus;
import com.orderhub.order.entity.Order;
import com.orderhub.order.repository.OrderRepo;
import com.orderhub.payment.DTO.PaymentResponse;
import com.orderhub.payment.entity.Payment;
import com.orderhub.payment.paymentGateway.GatewayPayment;
import com.orderhub.payment.paymentGateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepo orderRepository;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate tx;

    public PaymentResponse pay(String email, Long orderId, String idempotencyKey) {
        Started started;
        try {
            started = tx.execute(s -> start(email, orderId, idempotencyKey));
        } catch (DataIntegrityViolationException e) {
            // safety net: the unique constraint stopped a duplicate insert
            return tx.execute(s -> replayResponse(
                    paymentRepository.findByUserEmailAndIdempotencyKey(email, idempotencyKey).orElseThrow(),
                    orderId));
        }
        if (started.replay() != null) {
            return started.replay();                       // same key as before: nothing new happens
        }

        GatewayPayment created;
        try {
            created = paymentGateway.createPayment(orderId, started.amount(), idempotencyKey);   // outside any transaction
        } catch (RuntimeException e) {
            log.error("Gateway createPayment failed for payment {}", started.paymentId(), e);
            tx.executeWithoutResult(s ->
                    paymentRepository.transition(started.paymentId(), PaymentStatus.PENDING, PaymentStatus.FAILED));
            throw new GatewayUnavailableException();
        }

        return tx.execute(s -> {
            Payment p = paymentRepository.findById(started.paymentId()).orElseThrow();
            p.setProviderRef(created.providerRef());
            return PaymentResponse.from(p);                // still PENDING: the webhook decides the outcome
        });
    }

    private Started start(String email, Long orderId, String key) {
        // 1. Lock the order FIRST, before any plain read (see the note below)
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(o -> o.getUser().getEmail().equals(email))
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // 2. Same key as before? Return the stored result (this comes before the status check)
        Optional<Payment> existing = paymentRepository.findByUserEmailAndIdempotencyKey(email, key);
        if (existing.isPresent()) {
            return new Started(null, null, replayResponse(existing.get(), orderId));
        }

        // 3. Is the order payable?
        if (order.getStatus() != OrderStatus.CREATED || !order.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Order is not payable");
        }
        if (paymentRepository.existsByOrderIdAndStatusIn(
                orderId, List.of(PaymentStatus.PENDING, PaymentStatus.SUCCESS))) {
            throw new ConflictException("A payment for this order is already in progress or completed");
        }

        // 4. Insert the PENDING payment; the amount always comes from the order
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(key);
        paymentRepository.saveAndFlush(payment);

        return new Started(payment.getId(), order.getTotalAmount(), null);
    }

    private PaymentResponse replayResponse(Payment existing, Long orderId) {
        if (!existing.getOrder().getId().equals(orderId)) {
            throw new ConflictException("Idempotency-Key was already used for a different order");
        }
        return PaymentResponse.from(existing);
    }

    private record Started(Long paymentId, BigDecimal amount, PaymentResponse replay) {}
}
