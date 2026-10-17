package com.orderhub.payment;

import com.orderhub.payment.entity.Payment;
import com.orderhub.payment.paymentGateway.GatewayStatus;
import com.orderhub.payment.paymentGateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMaintenanceJob {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentResultService paymentResultService;

    @Value("${orderhub.payment.pending-timeout-minutes}")
    private long pendingTimeoutMinutes;

    // payments still PENDING after a while: ask the gateway what really happened
    @Scheduled(fixedDelay = 60_000)
    public void reconcilePending() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        for (Long id : paymentRepository.findIdsByStatusAndCreatedBefore(PaymentStatus.PENDING, before)) {
            try {
                Payment p = paymentRepository.findById(id).orElseThrow();
                GatewayStatus status = p.getProviderRef() == null
                        ? GatewayStatus.FAILED                       // we never got a reference from the gateway
                        : paymentGateway.fetchStatus(p.getProviderRef());
                paymentResultService.reconcile(id, status);
            } catch (Exception e) {
                log.warn("Could not reconcile payment {}", id, e);   // one failure must not stop the others
            }
        }
    }

    // refunds happen outside any database transaction
    @Scheduled(fixedDelay = 60_000)
    public void processRefunds() {
        for (Long id : paymentRepository.findIdsByStatus(PaymentStatus.REFUND_PENDING)) {
            try {
                Payment p = paymentRepository.findById(id).orElseThrow();
                paymentGateway.refund(p.getProviderRef(), p.getAmount());
                paymentResultService.markRefunded(id);
            } catch (Exception e) {
                log.warn("Refund for payment {} failed, will retry", id, e);   // stays REFUND_PENDING
            }
        }
    }
}
