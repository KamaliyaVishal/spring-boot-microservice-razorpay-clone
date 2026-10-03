package com.payment_service.service.Impl;

import com.common_lib.dto.FindOrCreateCustomerRequest;
import com.common_lib.enums.EventAggregateType;
import com.common_lib.enums.OrderStatus;
import com.common_lib.exception.BusinessRuleViolationException;
import com.common_lib.exception.DuplicateResourceException;
import com.common_lib.exception.ResourceNotFoundException;
import com.payment_service.client.CustomerServiceClient;
import com.payment_service.dto.request.CreateOrderRequest;
import com.payment_service.dto.response.OrderResponse;
import com.payment_service.dto.response.PaymentResponse;
import com.payment_service.entity.OrderRecord;
import com.payment_service.mapper.GlobalPaymentMapper;
import com.payment_service.outbox.OutboxEventPublisher;
import com.payment_service.repository.OrderRepository;
import com.payment_service.repository.PaymentRepository;
import com.payment_service.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final GlobalPaymentMapper mapper;
    private final OutboxEventPublisher outboxEventPublisher;
    private final CustomerServiceClient customerServiceClient;

    @Value("${payment.order.default-order-expiry-minutes:30}")
    private int defaultOrderExpiryMinutes;

    @Override
    @CircuitBreaker(name = "merchant-service")
    @Retry(name = "merchant-service")
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(UUID merchantId, CreateOrderRequest request) {

        if (request.receipt() != null && orderRepository.existsByMerchantIdAndReceipt(merchantId, request.receipt()))
            throw new DuplicateResourceException("Order with receipt already exists", "Receipt", request.receipt());

        UUID customerId = null;
        if (request.customer() != null) {
            customerId = customerServiceClient.findOrCreate(
                    new FindOrCreateCustomerRequest(merchantId,
                            request.customer().email(),
                            request.customer().name(),
                            request.customer().phone())
            );
        }

        OrderRecord order = OrderRecord.builder()
                .receipt(request.receipt())
                .amount(request.amount())
                .notes(request.notes())
                .merchantId(merchantId)
                .customerId(customerId)
                .status(OrderStatus.CREATED)
                .expiredAt(request.expiresAt() != null
                        ? request.expiresAt()
                        : LocalDateTime.now().plusMinutes(defaultOrderExpiryMinutes))
                .build();
        order = orderRepository.save(order);

        outboxEventPublisher.publish(EventAggregateType.ORDER, order.getId(), "ORDER_CREATED",
                Map.of("orderId", order.getId().toString(),
                        "merchantId", merchantId.toString(),
                        "orderStatus", order.getStatus().name(),
                        "amountUnits", order.getAmount().getAmountUnits(),
                        "amountCurrency", order.getAmount().getCurrency()
                )
        );

        return mapper.toOrderResponse(order);
    }

    private OrderRecord findOrderById(UUID merchantId, UUID orderId) {
        return orderRepository.findByMerchantIdAndId(merchantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderId", orderId));
    }

    @Override
    public OrderResponse getOrderById(UUID merchantId, UUID orderId) {
        return mapper.toOrderResponse(findOrderById(merchantId, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(UUID merchantId, UUID orderId) {

        OrderRecord order = findOrderById(merchantId, orderId);

        if (Set.of(OrderStatus.CANCELLED, OrderStatus.PAID).contains(order.getStatus()))
            throw new BusinessRuleViolationException("This order cannot be cancelled because it is already paid or cancelled",
                    "OrderStatus", order.getStatus());

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        outboxEventPublisher.publish(EventAggregateType.ORDER, order.getId(), "ORDER_CANCELLED",
                Map.of("orderId", order.getId(),
                        "merchantId", merchantId.toString(),
                        "orderStatus", order.getStatus().name(),
                        "amountUnits", order.getAmount().getAmountUnits(),
                        "amountCurrency", order.getAmount().getCurrency()
                )
        );
        return mapper.toOrderResponse(order);
    }

    @Override
    public List<PaymentResponse> listPayments(UUID merchantId, UUID orderId) {
        //OrderRecord orderRecord = findOrderById(merchantId, orderId);
        return mapper.toPaymentResponseList(paymentRepository.findAllByOrderRecord_Id(orderId));
    }
}
