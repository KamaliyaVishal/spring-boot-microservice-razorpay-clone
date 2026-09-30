package com.payment_service.mapper;

import com.payment_service.dto.response.OrderResponse;
import com.payment_service.dto.response.PaymentResponse;
import com.payment_service.entity.OrderRecord;
import com.payment_service.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GlobalPaymentMapper {

    OrderResponse toOrderResponse(OrderRecord orderRecord);

    @Mapping(target = "orderId", source = "orderRecord.id")
    PaymentResponse toPaymentResponse(Payment payment);

    List<PaymentResponse> toPaymentResponseList(List<Payment> payments);

}
