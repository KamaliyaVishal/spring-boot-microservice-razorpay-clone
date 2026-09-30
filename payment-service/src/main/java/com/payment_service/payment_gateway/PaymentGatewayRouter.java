package com.payment_service.payment_gateway;

import com.common_lib.enums.PaymentMethod;
import com.common_lib.exception.BusinessRuleViolationException;
import com.payment_service.payment_gateway.dto.PaymentRequest;
import com.payment_service.payment_gateway.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final Map<PaymentMethod, PaymentAdapter> paymentAdapterMap;

    public PaymentResult routeInitiatePaymentStrategy(PaymentRequest request) {

        PaymentAdapter paymentAdapter = getPaymentAdapter(request.paymentMethod());
        return paymentAdapter.initiatePayment(request);
    }

    private PaymentAdapter getPaymentAdapter(PaymentMethod paymentMethod) {
        PaymentAdapter paymentAdapter = paymentAdapterMap.get(paymentMethod);
        if (paymentAdapter == null)
            throw new BusinessRuleViolationException("No Payment gateway method register fot method" + paymentMethod,
                    "Payment Method", paymentMethod);
        return paymentAdapter;
    }

    public PaymentResult routeCapturePaymentStrategy(PaymentMethod paymentMethod, UUID paymentId) {

        PaymentAdapter paymentAdapter = getPaymentAdapter(paymentMethod);
        return paymentAdapter.capturePayment(paymentId);
    }
}
