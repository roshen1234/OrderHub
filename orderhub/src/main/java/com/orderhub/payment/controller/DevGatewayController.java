package com.orderhub.payment.controller;

import com.orderhub.payment.paymentGateway.GatewayStatus;
import com.orderhub.payment.paymentGateway.MockPaymentGateway;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

//Just for now usally this will be done by razor pay etc
@RestController
@RequestMapping("/dev/gateway")
@ConditionalOnProperty(name = "orderhub.payment.gateway", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class DevGatewayController {

    private final MockPaymentGateway gateway;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${server.port:8080}")
    private int port;

    public record SimulateRequest(@NotBlank String providerRef, @NotNull Boolean success,
                                  String eventId, Boolean sendWebhook) {}

    @PostMapping("/simulate")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> simulate(@Valid @RequestBody SimulateRequest r){
        gateway.markStatus(r.providerRef(), r.success() ? GatewayStatus.PAID : GatewayStatus.FAILED);

        if (Boolean.FALSE.equals(r.sendWebhook())) {
            return ResponseEntity.ok("Gateway state changed, no webhook sent");
        }

        String body = objectMapper.writeValueAsString(Map.of(
                "eventId", r.eventId() != null ? r.eventId() : "evt_" + UUID.randomUUID(),
                "type", r.success() ? "payment.succeeded" : "payment.failed",
                "providerRef", r.providerRef()));

        try {
            restClient.post()
                    .uri("http://localhost:" + port + "/payments/webhook")      // the real endpoint
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Signature", gateway.sign(body))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return ResponseEntity.ok("Webhook accepted");
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body("Webhook answered " + e.getStatusCode());
        }
    }
}
