package com.payment_service.payment_gateway.dto;

public sealed interface PaymentResult permits PaymentResult.Pending, PaymentResult.Failure, PaymentResult.Success {

    record Pending(
            String registrationRef
    ) implements PaymentResult {
    }

    record Failure(
            String errorCode,
            String errorDescription
    ) implements PaymentResult {
    }

    record Success(
            String bankReference
    ) implements PaymentResult {
    }

}
