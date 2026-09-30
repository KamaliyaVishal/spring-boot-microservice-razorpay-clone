package com.merchant_service.dto.request;

import com.common_lib.enums.Environment;
import jakarta.validation.constraints.NotNull;

public record CreateApiKeyRequest(

        @NotNull(message = "Environment cannot be null")
        Environment environment
) {
}
