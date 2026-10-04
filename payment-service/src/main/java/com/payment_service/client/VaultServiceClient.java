package com.payment_service.client;

import com.common_lib.dto.VaultChargeRequest;
import com.payment_service.payment_processor.dto.PaymentProcessorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "vault-service", path = "/internal/vault", url = "${VAULT_SERVICE_URI:}")
public interface VaultServiceClient {

    @PostMapping("/charge")
    PaymentProcessorResponse charge(@RequestBody VaultChargeRequest request);
}
