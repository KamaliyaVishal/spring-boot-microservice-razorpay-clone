package com.merchant_service.dto.response;


import com.common_lib.enums.Environment;

import java.util.UUID;

public record CreateApiKeyResponse(
        UUID id,
        String keyId,
        String keySecretHash,
        Environment environment
) {}
