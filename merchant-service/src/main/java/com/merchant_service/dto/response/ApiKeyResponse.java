package com.merchant_service.dto.response;

import com.common_lib.enums.Environment;

import java.time.LocalDateTime;

public record ApiKeyResponse(
        String keyId,
        Environment environment,
        LocalDateTime lastUsedAt,
        LocalDateTime rotatedAt
) {}
