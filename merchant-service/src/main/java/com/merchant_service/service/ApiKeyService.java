package com.merchant_service.service;

import com.merchant_service.dto.request.CreateApiKeyRequest;
import com.merchant_service.dto.response.ApiKeyResponse;
import com.merchant_service.dto.response.CreateApiKeyResponse;
import com.merchant_service.dto.response.DeleteResponse;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyService {
    CreateApiKeyResponse create(UUID merchantId, CreateApiKeyRequest request);

    List<ApiKeyResponse> fetchAllApiKeys(UUID merchantId);

    DeleteResponse revokeApiKeyByMerchantId(UUID merchantId, String keyId);

    CreateApiKeyResponse rotateApiKeyByMerchantId(UUID merchantId, String keyId);
}
