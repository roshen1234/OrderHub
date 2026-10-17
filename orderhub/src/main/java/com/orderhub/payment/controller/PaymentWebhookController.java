package com.orderhub.payment.controller;

import com.orderhub.payment.PaymentResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentResultService paymentResultService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody, @RequestHeader HttpHeaders headers) {
        paymentResultService.handleWebhook(rawBody, headers);
        return ResponseEntity.ok().build();
    }
}