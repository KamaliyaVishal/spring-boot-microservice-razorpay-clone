package com.payment_service.payment_processor;

import com.payment_service.payment_processor.dto.PaymentProcessorRequest;
import com.payment_service.payment_processor.dto.PaymentProcessorResponse;

public interface PaymentProcessor {

    PaymentProcessorResponse charge(PaymentProcessorRequest request);

}
