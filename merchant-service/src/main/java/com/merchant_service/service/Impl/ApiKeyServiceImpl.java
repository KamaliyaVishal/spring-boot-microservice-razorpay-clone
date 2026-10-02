package com.merchant_service.service.Impl;

import com.common_lib.cache.ApiKeyCache;
import com.common_lib.exception.BusinessRuleViolationException;
import com.common_lib.exception.ResourceNotFoundException;
import com.common_lib.util.RandomizerUtil;
import com.merchant_service.dto.request.CreateApiKeyRequest;
import com.merchant_service.dto.response.ApiKeyResponse;
import com.merchant_service.dto.response.CreateApiKeyResponse;
import com.merchant_service.dto.response.DeleteResponse;
import com.merchant_service.entity.ApiKey;
import com.merchant_service.entity.Merchant;
import com.merchant_service.mapper.GlobalMerchantMapper;
import com.merchant_service.repository.ApiKeyRepository;
import com.merchant_service.repository.MerchantRepository;
import com.merchant_service.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final GlobalMerchantMapper mapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    private final ApiKeyCache apiKeyCache;

    @Value("${app.api-key.keyId-length:24}")
    private Integer keyIdLength;

    @Value("${app.api-key.rawSecret-length:40}")
    private Integer rawSecretLength;

    @Value("${app.api-key.gracePeriod-expiry-time-in-hour:24}")
    private Integer gracePeriodTime;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateApiKeyResponse create(UUID merchantId, CreateApiKeyRequest request) {

        // Don't require @Lock(LockModeType.PESSIMISTIC_WRITE) as unique key constraints
        // won't let concurrent requests generate duplicate keys; instead, it will throw a Duplicate Resource exception.
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        String keyId = String.join(
                "_",
                "rzp",
                request.environment().name().toLowerCase(),
                RandomizerUtil.randomBase64(keyIdLength)
        );

        String rawSecret = RandomizerUtil.randomBase64(rawSecretLength);

        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyId(keyId)
                .keySecretHash(bCryptPasswordEncoder.encode(rawSecret))
                .environment(request.environment())
                .build();

        apiKeyRepository.save(apiKey);

        return mapper.toCreateApiKeyResponse(apiKey);
    }

    @Override
    public List<ApiKeyResponse> fetchAllApiKeys(UUID merchantId) {
        return mapper.toApiKeyResponseList(apiKeyRepository.findAllByMerchantId(merchantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResponse revokeApiKeyByMerchantId(UUID merchantId, String keyId) {

        ApiKey apiKey = apiKeyRepository.findByMerchant_IdAndKeyId(merchantId, keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API_Key", keyId));

        apiKey.setEnabled(false);

        // Redis cache evict on revoke key
        apiKeyCache.evict(apiKey.getKeyId());

        // Optional: apiKeyRepository.save(apiKey);
        // When this method ends, @Transactional commits,
        // dirty checking triggers, and the UPDATE SQL runs.
        apiKeyRepository.save(apiKey);

        return DeleteResponse.fromEntity(apiKey, keyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateApiKeyResponse rotateApiKeyByMerchantId(UUID merchantId, String keyId) {

        ApiKey apiKey = apiKeyRepository.findByMerchant_IdAndKeyId(merchantId, keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API_Key", keyId));

        if (!apiKey.isEnabled())
            throw new BusinessRuleViolationException("Cannot rotate API key [%s] because it is disabled or revoked.".formatted(keyId),
                    "keyId", keyId);

        String newRawSecret = RandomizerUtil.randomBase64(rawSecretLength);
        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(bCryptPasswordEncoder.encode(newRawSecret));
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setGracePeriodExpiredAt(LocalDateTime.now().plusHours(gracePeriodTime));

        // Redis cache evict on rotate key
        apiKeyCache.evict(apiKey.getKeyId());

        apiKeyRepository.save(apiKey);

        return mapper.toCreateApiKeyResponse(apiKey);
    }
}
