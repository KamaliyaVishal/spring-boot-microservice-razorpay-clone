package com.payment_service.config;

import com.common_lib.enums.PaymentMethod;
import com.payment_service.payment_gateway.PaymentAdapter;
import com.payment_service.payment_gateway.adapter.CardPaymentAdapter;
import com.payment_service.payment_gateway.adapter.NetBakingPaymentAdapter;
import com.payment_service.payment_gateway.adapter.UpiPaymentAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentAdapterConfig {

    private final CardPaymentAdapter cardPaymentAdapter;
    private final NetBakingPaymentAdapter netBakingPaymentAdapter;
    private final UpiPaymentAdapter upiPaymentAdapter;

    @Bean
    public Map<PaymentMethod, PaymentAdapter> paymentAdapterMap() {
        return Map.of(
                PaymentMethod.CARD, cardPaymentAdapter,
                PaymentMethod.NETBANKING, netBakingPaymentAdapter,
                PaymentMethod.UPI, upiPaymentAdapter
        );
    }
}
