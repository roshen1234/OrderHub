package com.orderhub.payment.controller;

import com.orderhub.common.exception.BadRequestException;
import com.orderhub.payment.DTO.PaymentRequest;
import com.orderhub.payment.DTO.PaymentResponse;
import com.orderhub.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {

        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new BadRequestException("Idempotency-Key must be between 1 and 100 characters");
        }

        PaymentResponse response =
                paymentService.pay(authentication.getName(), request.orderId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

