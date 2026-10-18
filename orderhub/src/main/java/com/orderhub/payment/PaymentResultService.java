package com.orderhub.payment;

import com.orderhub.order.OrderStatus;
import com.orderhub.order.entity.OrderStatusHistory;
import com.orderhub.order.repository.OrderRepo;
import com.orderhub.order.repository.OrderStatusHistoryRepo;
import com.orderhub.payment.entity.Payment;
import com.orderhub.payment.paymentGateway.GatewayStatus;
import com.orderhub.payment.paymentGateway.PaymentGateway;
import com.orderhub.payment.paymentGateway.WebhookEvent;
import com.orderhub.payment.repositories.PaymentRepository;
import com.orderhub.payment.repositories.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentResultService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepo orderRepository;
    private final OrderStatusHistoryRepo historyRepository;

    @Transactional
    public void handleWebhook(String rawBody, HttpHeaders headers) {
        WebhookEvent event = paymentGateway.parseWebhook(rawBody, headers);    // throws InvalidWebhookException

        // the duplicate check and the work are in ONE transaction: if the work fails, the event
        // is not marked as processed, so the gateway's retry gets handled
        if (processedEventRepository.insertIfAbsent(event.eventId()) == 0) {
            log.info("Duplicate webhook event {}", event.eventId());
            return;
        }

        Optional<Payment> found = paymentRepository.findByProviderRef(event.providerRef());
        if (found.isEmpty()) {
            log.warn("Webhook for unknown payment {}", event.providerRef());
            return;                                                             // reply 200 anyway
        }
        Payment payment = found.get();

        switch (event.type()) {
            case PAYMENT_SUCCEEDED -> applySuccess(payment);
            case PAYMENT_FAILED    -> applyFailure(payment);
            case REFUND_PROCESSED  -> paymentRepository.transition(
                    payment.getId(), PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED);
            case IGNORED           -> { }
        }
    }

    @Transactional
    public void reconcile(Long paymentId, GatewayStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        if (status == GatewayStatus.PAID) applySuccess(payment);
        else if (status == GatewayStatus.FAILED) applyFailure(payment);
    }

    @Transactional
    public void markRefunded(Long paymentId) {
        paymentRepository.transition(paymentId, PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED);
    }

    private void applySuccess(Payment payment) {
        // only one caller (webhook or reconciliation) can move the payment out of PENDING
        if (paymentRepository.transition(payment.getId(), PaymentStatus.PENDING, PaymentStatus.SUCCESS) == 0) {
            return;
        }
        Long orderId = payment.getOrder().getId();
        if (orderRepository.transitionBeforeExpiry(
                orderId, OrderStatus.CREATED, OrderStatus.PAID, LocalDateTime.now()) == 1) {
            OrderStatusHistory h = new OrderStatusHistory();
            h.setOrder(payment.getOrder());
            h.setFromStatus(OrderStatus.CREATED);
            h.setToStatus(OrderStatus.PAID);
            historyRepository.save(h);
        } else {
            // the order expired or was cancelled while the customer was paying: money must go back
            log.warn("Order {} not payable after successful payment {}; marking for refund", orderId, payment.getId());
            paymentRepository.transition(payment.getId(), PaymentStatus.SUCCESS, PaymentStatus.REFUND_PENDING);
        }
    }

    private void applyFailure(Payment payment) {
        paymentRepository.transition(payment.getId(), PaymentStatus.PENDING, PaymentStatus.FAILED);
    }
}
