package com.orderhub.payment.paymentGateway;


import com.orderhub.common.exception.InvalidWebhookException;
import com.orderhub.payment.DTO.GatewayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "orderhub.payment.gateway", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Map<String, GatewayStatus> states = new ConcurrentHashMap<>();
    private final Map<String, String> refsByKey = new ConcurrentHashMap<>();

    public MockPaymentGateway(ObjectMapper objectMapper,
                              @Value("${orderhub.payment.webhook-secret}") String secret) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public GatewayPayment createPayment(Long orderId, BigDecimal amount, String idempotencyKey) {
        // like a real gateway: the same idempotency key gives back the same payment
        String ref = refsByKey.computeIfAbsent(orderId + ":" + idempotencyKey, k -> "mock_" + UUID.randomUUID());
        states.putIfAbsent(ref, GatewayStatus.PENDING);
        log.info("Mock createPayment: order={}, amount={}, ref={}", orderId, amount, ref);
        return new GatewayPayment(ref);
    }

    @Override
    public WebhookEvent parseWebhook(String rawBody, HttpHeaders headers) {
        String signature = headers.getFirst("X-Signature");
        if (signature == null || !MessageDigest.isEqual(
                sign(rawBody).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidWebhookException("Invalid signature");
        }
        try {
            JsonNode n = objectMapper.readTree(rawBody);
            String eventId = n.path("eventId").asText("");
            String ref = n.path("providerRef").asText("");
            if (eventId.isBlank() || eventId.length() > 100 || ref.isBlank()) {
                throw new InvalidWebhookException("Malformed event");
            }
            WebhookEventType type = switch (n.path("type").asText("")) {
                case "payment.succeeded" -> WebhookEventType.PAYMENT_SUCCEEDED;
                case "payment.failed"    -> WebhookEventType.PAYMENT_FAILED;
                case "refund.processed"  -> WebhookEventType.REFUND_PROCESSED;
                default                  -> WebhookEventType.IGNORED;
            };
            return new WebhookEvent(eventId, type, ref);
        } catch (Exception e) {
            throw new InvalidWebhookException("Malformed JSON");
        }
    }

    @Override
    public void refund(String providerRef, BigDecimal amount) {
        log.info("Mock refund: ref={}, amount={}", providerRef, amount);
    }

    @Override
    public GatewayStatus fetchStatus(String providerRef) {
        return states.getOrDefault(providerRef, GatewayStatus.PENDING);
    }

    // helpers used only by the dev controller
    public void markStatus(String providerRef, GatewayStatus status) { states.put(providerRef, status); }

    public String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
