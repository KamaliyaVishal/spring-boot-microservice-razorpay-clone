package com.payment_service.payment_gateway;

import com.payment_service.payment_gateway.dto.PaymentRequest;
import com.payment_service.payment_gateway.dto.PaymentResult;

import java.util.UUID;

public interface PaymentAdapter {
    PaymentResult initiatePayment(PaymentRequest paymentRequest);

    PaymentResult capturePayment(UUID paymentId);
}
