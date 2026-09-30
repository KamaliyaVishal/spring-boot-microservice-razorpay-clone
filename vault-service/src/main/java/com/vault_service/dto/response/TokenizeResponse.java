package com.vault_service.dto.response;

import com.common_lib.enums.CardType;

public record TokenizeResponse(
        String token,
        String lastFour,
        CardType cardType,
        Integer expiryMonth,
        Integer expiryYear
) {
}
