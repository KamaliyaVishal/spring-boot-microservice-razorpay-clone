package com.payment_service.payment_gateway.adapter;

import com.common_lib.dto.VaultChargeRequest;
import com.payment_service.client.VaultServiceClient;
import com.payment_service.payment_gateway.PaymentAdapter;
import com.payment_service.payment_gateway.dto.PaymentRequest;
import com.payment_service.payment_gateway.dto.PaymentResult;
import com.payment_service.payment_processor.dto.PaymentProcessorResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CardPaymentAdapter implements PaymentAdapter {

    private final VaultServiceClient vaultServiceClient;

    @Override
    @CircuitBreaker(name = "vault-service")
    @Retry(name = "vault-service")
    public PaymentResult initiatePayment(PaymentRequest request) {

        String token = request.methodDetails().get("token").toString();

        PaymentProcessorResponse response = vaultServiceClient
                .charge(new VaultChargeRequest(
                        request.paymentId(),
                        token,
                        request.amount(),
                        request.methodDetails())
                );

        return switch (response) {
            case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.bankReference());
            case PaymentProcessorResponse.Failure failure ->
                    new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
            case PaymentProcessorResponse.Pending pending -> new PaymentResult.Pending(pending.processorReference());
        };
    }

    @Override
    public PaymentResult capturePayment(UUID paymentId) {
        return new PaymentResult.Success("CARD_REF");
    }
}
