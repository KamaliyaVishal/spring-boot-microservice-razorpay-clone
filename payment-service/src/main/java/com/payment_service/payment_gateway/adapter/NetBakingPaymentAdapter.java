package com.payment_service.payment_gateway.adapter;

import com.common_lib.enums.PaymentMethod;
import com.payment_service.payment_gateway.PaymentAdapter;
import com.payment_service.payment_gateway.dto.PaymentRequest;
import com.payment_service.payment_gateway.dto.PaymentResult;
import com.payment_service.payment_processor.PaymentProcessorRouter;
import com.payment_service.payment_processor.dto.PaymentProcessorRequest;
import com.payment_service.payment_processor.dto.PaymentProcessorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NetBakingPaymentAdapter implements PaymentAdapter {

    private final PaymentProcessorRouter paymentProcessorRouter;

    @Override
    public PaymentResult initiatePayment(PaymentRequest request) {

        log.info("Initiating request for NetBaking payment with PaymentId: {}", request.paymentId());

        try {
            PaymentProcessorRequest paymentProcessorRequest = PaymentProcessorRequest.nonCard(
                    request.paymentId(),
                    PaymentMethod.NETBANKING,
                    request.amount(),
                    request.methodDetails()
            );

            PaymentProcessorResponse paymentProcessorResponse =
                    paymentProcessorRouter.routeToDedicatedPaymentProcessor(paymentProcessorRequest);

            return switch (paymentProcessorResponse) {
                case PaymentProcessorResponse.Pending pending ->
                        new PaymentResult.Pending(pending.processorReference());
                case PaymentProcessorResponse.Failure failure ->
                        new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
                case PaymentProcessorResponse.Success success ->
                        new PaymentResult.Success(success.bankReference());
            };
        } catch (Exception e) {
            log.warn("Payment request with NetBanking failed with PaymentId:{}", request.paymentId());
            return new PaymentResult.Failure("NBK_FAILED", e.getMessage());
        }
    }

    @Override
    public PaymentResult capturePayment(UUID paymentId) {
        return new PaymentResult.Success("NBK_REF");
    }
}