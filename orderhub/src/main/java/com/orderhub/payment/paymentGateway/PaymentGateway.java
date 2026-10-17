package com.orderhub.payment.paymentGateway;

import com.orderhub.payment.DTO.GatewayResult;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayPayment createPayment(Long orderId, BigDecimal amount, String idempotencyKey);

    /** Verifies the signature and turns the gateway's JSON into our own event. Throws InvalidWebhookException. */
    WebhookEvent parseWebhook(String rawBody, HttpHeaders headers);

    void refund(String providerRef, BigDecimal amount);

    GatewayStatus fetchStatus(String providerRef);

}
