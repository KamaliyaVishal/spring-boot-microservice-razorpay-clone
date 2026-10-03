package com.payment_service.saga;

import com.common_lib.enums.EventAggregateType;
import com.common_lib.enums.OrderStatus;
import com.common_lib.enums.PaymentEvent;
import com.common_lib.enums.PaymentStatus;
import com.common_lib.exception.BusinessRuleViolationException;
import com.common_lib.exception.ResourceNotFoundException;
import com.payment_service.dto.request.PaymentInitRequest;
import com.payment_service.dto.response.PaymentResponse;
import com.payment_service.entity.OrderRecord;
import com.payment_service.entity.Payment;
import com.payment_service.mapper.GlobalPaymentMapper;
import com.payment_service.outbox.OutboxEventPublisher;
import com.payment_service.payment_gateway.dto.PaymentResult;
import com.payment_service.payment_transition.PaymentTransitionService;
import com.payment_service.repository.OrderRepository;
import com.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentAuthorizationRecorder {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransitionService paymentTransitionService;
    private final OutboxEventPublisher eventPublisher;
    private final GlobalPaymentMapper mapper;

    @Transactional
    public Payment recordPayment(UUID merchantId, PaymentInitRequest request, String idempotencyKey) {
        OrderRecord order = orderRepository.findByMerchantIdAndIdForUpdate(request.orderId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.ATTEMPTED)
            throw new BusinessRuleViolationException("Order cannot accept payment in status: " + order.getStatus(),
                    "Invalid Order Status", order.getStatus());

        order.setStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts() + 1);

        Payment payment = Payment.builder()
                .orderRecord(order)
                .merchantId(merchantId)
                .amount(order.getAmount())
                .status(PaymentStatus.CREATED)
                .paymentMethod(request.method())
                .idempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
                .methodDetails(request.methodDetails())
                .build();
        payment = paymentRepository.save(payment);
        paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_ATTEMPT);
        return payment;
    }

    @Transactional
    public Optional<PaymentResponse> findExistingAttempt(UUID merchantId, String idempotencyKey) {
        return paymentRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
                .map(mapper::toPaymentResponse);
    }

    @Transactional
    public PaymentResponse compensateAuthorizationFailure(UUID paymentId, String errorCode,
                                                          String errorDescription) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
        payment.setErrorCode(errorCode);
        payment.setErrorDescription(errorDescription);
        payment = paymentRepository.save(payment);

        publishStatusEvent(payment, "PAYMENT_AUTHORIZATION_COMPENSATED");
        return mapper.toPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse applyGatewayResult(UUID paymentId, PaymentResult result) {
        log.info("Applying Gateway result for paymentId: {}", paymentId);
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        switch (result) {
            case PaymentResult.Pending pending -> payment.setProcessorReference(pending.registrationRef());
            case PaymentResult.Failure failure -> {
                paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
            case PaymentResult.Success success ->
                    log.warn("Invalid state: initiate() gateway call returned Success directly, paymentId={}", paymentId);
        }

        payment = paymentRepository.save(payment);
        publishStatusEvent(payment, "PAYMENT_CREATED");
        log.info("Successfully applied Gateway result for paymentId: {}", paymentId);
        return mapper.toPaymentResponse(payment);
    }

    private void publishStatusEvent(Payment payment, String eventType) {
        eventPublisher.publish(EventAggregateType.PAYMENT, payment.getId(), eventType,
                Map.of("orderId", payment.getOrderRecord().getId().toString(),
                        "paymentId", payment.getId().toString(),
                        "merchantId", payment.getMerchantId().toString(),
                        "paymentStatus", payment.getStatus().name(),
                        "amountUnits", payment.getAmount().getAmountUnits(),
                        "amountCurrency", payment.getAmount().getCurrency(),
                        "paymentMethod", payment.getPaymentMethod()
                ));
    }
}
