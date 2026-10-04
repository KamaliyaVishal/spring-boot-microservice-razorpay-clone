package com.payment_service.service.Impl;

import com.common_lib.enums.EventAggregateType;
import com.common_lib.enums.OrderStatus;
import com.payment_service.dto.request.CreateOrderRequest;
import com.payment_service.dto.response.OrderResponse;
import com.payment_service.entity.OrderRecord;
import com.payment_service.mapper.GlobalPaymentMapper;
import com.payment_service.outbox.OutboxEventPublisher;
import com.payment_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OutboxEventPublisher eventPublisher;
    private final GlobalPaymentMapper mapper;

    @Transactional
    public OrderResponse persist(UUID merchantId, CreateOrderRequest request, UUID customerId,
                                 int defaultOrderExpiryMinutes) {
        OrderRecord order = OrderRecord.builder()
                .receipt(request.receipt())
                .amount(request.amount())
                .notes(request.notes())
                .merchantId(merchantId)
                .customerId(customerId)
                .status(OrderStatus.CREATED)
                .expiredAt(request.expiresAt() != null ? request.expiresAt() :
                        LocalDateTime.now().plusMinutes(defaultOrderExpiryMinutes))
                .build();

        order = orderRepository.save(order);

        eventPublisher.publish(EventAggregateType.ORDER, order.getId(), "ORDER_CREATED",
                Map.of("orderId", order.getId(),
                        "merchantId", merchantId.toString(),
                        "orderStatus", order.getStatus().name(),
                        "amountUnits", order.getAmount().getAmountUnits(),
                        "amountCurrency", order.getAmount().getCurrency()
                )
        );

        return mapper.toOrderResponse(order);
    }
}
