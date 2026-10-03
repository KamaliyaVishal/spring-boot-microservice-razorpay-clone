package com.payment_service.payment_transition;

import com.common_lib.context.MerchantContext;
import com.common_lib.enums.PaymentActor;
import com.common_lib.enums.PaymentEvent;
import com.common_lib.enums.PaymentStatus;
import com.payment_service.entity.Payment;
import com.payment_service.entity.PaymentTransitionLog;
import com.payment_service.repository.PaymentTransitionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransitionService {

    private final PaymentStateMachine paymentStateMachine;
    private final PaymentTransitionLogRepository paymentTransitionLogRepository;
    private final MerchantContext merchantContext;

    @Transactional(rollbackFor = Exception.class)
    public PaymentStatus apply(Payment payment, PaymentEvent paymentEvent) {

        PaymentStatus next = paymentStateMachine.transition(payment.getStatus(), paymentEvent);

        PaymentTransitionLog paymentTransitionLog = PaymentTransitionLog.builder()
                .payment(payment)
                .fromStatus(payment.getStatus())
                .paymentEvent(paymentEvent)
                .toStatus(next).paymentActor(getPaymentActor())
                .occurrenceAt(LocalDateTime.now())
                .build();
        paymentTransitionLogRepository.save(paymentTransitionLog);
        payment.setStatus(next);

        return next;
    }

    private PaymentActor getPaymentActor() {
        try {
            String keyId = merchantContext.getKeyId();
            UUID merchantId = merchantContext.getMerchantId();

            if (keyId != null && !keyId.isBlank()) return PaymentActor.CUSTOMER;
            else if (merchantId != null) return PaymentActor.MERCHANT;
        } catch (Exception ignored) {
        }
        return PaymentActor.SYSTEM;
    }

}
