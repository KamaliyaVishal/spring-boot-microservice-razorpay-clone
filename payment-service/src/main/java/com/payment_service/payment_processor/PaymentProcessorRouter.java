package com.payment_service.payment_processor;

import com.common_lib.enums.PaymentMethod;
import com.common_lib.exception.BusinessRuleViolationException;
import com.payment_service.payment_processor.dto.PaymentProcessorRequest;
import com.payment_service.payment_processor.dto.PaymentProcessorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentProcessorRouter {

    private final Map<PaymentMethod, PaymentProcessor> paymentProcessorMap;

    public PaymentProcessorResponse routeToDedicatedPaymentProcessor(PaymentProcessorRequest request) {

        PaymentProcessor paymentProcessor = paymentProcessorMap.get(request.method());

        if (paymentProcessor == null)
            throw new BusinessRuleViolationException("No payment processor register for method" + request.method(),
                    "Payment Method", request.method());

        return paymentProcessor.charge(request);
    }
}
