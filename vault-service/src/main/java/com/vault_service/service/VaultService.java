package com.vault_service.service;

import com.common_lib.entity.Money;
import com.vault_service.dto.request.TokenizeRequest;
import com.vault_service.dto.response.TokenizeResponse;

import java.util.Map;
import java.util.UUID;

public interface VaultService {

    TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId);

    PaymentProcessorResponse charge(UUID uuid, String token, Money amount, Map<String, Object> stringObjectMap);
}
