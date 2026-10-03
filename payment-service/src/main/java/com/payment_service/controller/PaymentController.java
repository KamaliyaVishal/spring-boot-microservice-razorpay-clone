package com.payment_service.controller;

import com.common_lib.context.MerchantContext;
import com.payment_service.dto.request.PaymentInitRequest;
import com.payment_service.dto.response.PaymentResponse;
import com.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody @Valid PaymentInitRequest request,
                                                           @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(merchantContext.getMerchantId(), request, idempotencyKey));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable UUID paymentId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(paymentService.capturePayment(merchantContext.getMerchantId(), paymentId));
    }

}
