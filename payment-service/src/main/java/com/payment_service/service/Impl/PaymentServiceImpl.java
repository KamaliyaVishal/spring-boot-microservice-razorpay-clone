package com.payment_service.service.Impl;

import com.common_lib.enums.EventAggregateType;
import com.common_lib.enums.OrderStatus;
import com.common_lib.enums.PaymentEvent;
import com.common_lib.enums.PaymentStatus;
import com.common_lib.exception.ResourceNotFoundException;
import com.payment_service.dto.request.PaymentInitRequest;
import com.payment_service.dto.response.PaymentResponse;
import com.payment_service.entity.OrderRecord;
import com.payment_service.entity.Payment;
import com.payment_service.mapper.GlobalPaymentMapper;
import com.payment_service.outbox.OutboxEventPublisher;
import com.payment_service.payment_gateway.PaymentGatewayRouter;
import com.payment_service.payment_gateway.dto.PaymentRequest;
import com.payment_service.payment_gateway.dto.PaymentResult;
import com.payment_service.payment_transition.PaymentTransitionService;
import com.payment_service.repository.OrderRepository;
import com.payment_service.repository.PaymentRepository;
import com.payment_service.saga.PaymentAuthorizationRecorder;
import com.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final GlobalPaymentMapper mapper;
    private final PaymentTransitionService paymentTransitionService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final PaymentAuthorizationRecorder paymentAuthorizationRecorder;

    /**
     * Why do we explicitly write rollbackFor = Exception.class?
     * By default, Spring’s standard @Transactional annotation only rolls back for unchecked exceptions (subclasses of RuntimeException and Error, like NullPointerException or IllegalArgumentException).
     * It will not roll back your database if a checked exception occurs (subclasses of Exception that you are forced to catch or declare, such as IOException, SQLException, or custom business exceptions).
     * By writing rollbackFor = Exception.class, you change this behavior to be 100% bulletproof. It forces Spring to roll back the database for every single type of exception—both checked and unchecked.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse initiatePayment(UUID merchantId, PaymentInitRequest request, String idempotencyKey) {

        if (idempotencyKey != null) {
            var existing = paymentAuthorizationRecorder.findExistingAttempt(merchantId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotency replay for paymentId: {}", existing.get().id());
                return existing.get();
            }
        }

        Payment payment = paymentAuthorizationRecorder.recordPayment(merchantId, request, idempotencyKey);

        PaymentRequest paymentRequest = new PaymentRequest(payment.getId(),
                request.orderId(), merchantId,
                payment.getAmount(), request.method(),
                request.methodDetails());

        PaymentResult result;
        try {
            result = paymentGatewayRouter.routeInitiatePaymentStrategy(paymentRequest);
        } catch (Exception e) {
            return paymentAuthorizationRecorder.compensateAuthorizationFailure(payment.getId(),
                    "PAYMENT_GATEWAY_ROUTER_UNREACHABLE", e.getMessage());
        }
        return paymentAuthorizationRecorder.applyGatewayResult(payment.getId(), result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse capturePayment(UUID merchantId, UUID paymentId) {

        //Payment payment = paymentRepository.findByMerchantIdAndId(merchantId, paymentId)
        //       .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        // @Lock(LockModeType.PESSIMISTIC_WRITE) : used to block concurrent updates on a specific database record.
        Payment payment = paymentRepository.findByMerchantIdAndIdForUpdate(merchantId, paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        // Do not set payment states directly; use the state machine instead to prevent unintended state transitions.
        paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);
        PaymentResult paymentResult = paymentGatewayRouter
                .routeCapturePaymentStrategy(payment.getPaymentMethod(), paymentId);

        switch (paymentResult) {
            case PaymentResult.Pending pending -> {

            }
            case PaymentResult.Failure failure -> {
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
                log.info("Payment failed while capturing for Payment : {}", paymentId);
            }
            case PaymentResult.Success success -> {
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
                payment.setCapturedAt(LocalDateTime.now());
                payment.setProcessorReference(success.bankReference());
                log.info("Payment captured for Payment : {}", paymentId);
            }
        }

        outboxEventPublisher.publish(EventAggregateType.PAYMENT, payment.getId(), "PAYMENT_STATUS_CHANGED",
                Map.of("orderId", payment.getOrderRecord().getId().toString(),
                        "paymentId", payment.getId().toString(),
                        "merchantId", merchantId.toString(),
                        "paymentStatus", payment.getStatus().name(),
                        "amountUnits", payment.getAmount().getAmountUnits(),
                        "amountCurrency", payment.getAmount().getCurrency(),
                        "paymentMethod", payment.getPaymentMethod()
                )
        );

        return mapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public void resolveAuthorization(UUID paymentId, boolean approve, String bankRef,
                                     String errorCode, String errorDescription) {

        //Payment payment = paymentRepository.findById(paymentId)
        //       .orElseThrow(() -> new ResourceNotFoundException("PaymentId", paymentId));

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentId", paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZING) {
            log.warn("Payment is not in Authorizing state, paymentID: {}, status: {}", paymentId, payment.getStatus());
            return;
        }

        OrderRecord orderRecord = payment.getOrderRecord();

        if (approve) {
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_SUCCESS);
            payment.setBankReference(bankRef);
            payment.setCapturedAt(LocalDateTime.now());

            //Auto-Capture
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);
            PaymentResult captureResult = paymentGatewayRouter
                    .routeCapturePaymentStrategy(payment.getPaymentMethod(), paymentId);

            switch (captureResult) {
                case PaymentResult.Success success -> {
                    paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
                    payment.setCapturedAt(LocalDateTime.now());
                    orderRecord.setStatus(OrderStatus.PAID);
                }
                case PaymentResult.Failure failure -> {
                    paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
                    payment.setErrorCode(failure.errorCode());
                    payment.setErrorDescription(failure.errorDescription());

                }
                default -> throw new IllegalStateException("Unexpected value: " + captureResult);
            }

        } else {
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
            payment.setErrorCode(errorCode);
            payment.setErrorDescription(errorDescription);
        }

        paymentRepository.save(payment);
        orderRepository.save(orderRecord);

        outboxEventPublisher.publish(EventAggregateType.PAYMENT, payment.getId(), "PAYMENT_STATUS_CHANGED",
                Map.of("orderId", payment.getOrderRecord().getId().toString(),
                        "paymentId", payment.getId().toString(),
                        "merchantId", payment.getMerchantId().toString(),
                        "paymentStatus", payment.getStatus().name(),
                        "amountUnits", payment.getAmount().getAmountUnits(),
                        "amountCurrency", payment.getAmount().getCurrency(),
                        "paymentMethod", payment.getPaymentMethod()
                )
        );
    }
}
