package com.vault_service.service.impl;

import com.common_lib.entity.Money;
import com.common_lib.enums.CardType;
import com.common_lib.exception.ResourceNotFoundException;
import com.common_lib.util.RandomizerUtil;
import com.vault_service.config.VaultEncryptionConfig;
import com.vault_service.dto.request.TokenizeRequest;
import com.vault_service.dto.response.TokenizeResponse;
import com.vault_service.entity.CardToken;
import com.vault_service.entity.VaultCard;
import com.vault_service.repository.CardTokenRepository;
import com.vault_service.repository.VaultCardRepository;
import com.vault_service.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaultServiceImpl implements VaultService {

    private final VaultCardRepository vaultCardRepository;
    private final CardTokenRepository cardTokenRepository;
    private final BytesEncryptor dekEncryptor;
    private final PaymentProcessorRouter paymentProcessorRouter;

    @Value("${app.vault.randomToken-length:32}")
    private Integer tokenLength;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId) {

        String pan = request.pan();
        String lastFour = pan.substring(pan.length() - 4);
        String bin = pan.substring(0, 6);
        CardType cardType = detectCardType(pan);

        byte[] dek = KeyGenerators.secureRandom(tokenLength).generateKey();
        byte[] encryptedPan = VaultEncryptionConfig.panEncryptor(dek)
                .encrypt(pan.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDek = dekEncryptor.encrypt(dek);

        VaultCard vaultCard = VaultCard.builder()
                .bin(Integer.parseInt(bin))
                .last4Digits(lastFour)
                .cardHolderName(request.cardHolderName())
                .encryptedDek(encryptedDek)
                .encryptedPan(encryptedPan)
                .cardType(cardType)
                .expiryMonth(request.expiryMonth())
                .expiryYear(request.expiryYear())
                .build();

        String randomToken = "tok_" + RandomizerUtil.randomBase64(tokenLength);

        CardToken cardToken = CardToken.builder()
                .vaultCard(vaultCard)
                .merchantId(merchantId)
                .token(randomToken)
                .customerId(request.customerId())
                .build();

        cardTokenRepository.save(cardToken);
        return new TokenizeResponse(randomToken, lastFour, cardType, request.expiryMonth(), request.expiryYear());
    }

    @Override
    public PaymentProcessorResponse charge(UUID paymentId, String token, Money amount, Map<String, Object> methodDetails) {

        CardToken cardToken = cardTokenRepository.findByTokenAndRevokedAtIsNull(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token", token));

        VaultCard vaultCard = cardToken.getVaultCard();
        byte[] panBytes = null;

        try {
            byte[] dek = dekEncryptor.decrypt(vaultCard.getEncryptedDek());
            panBytes = VaultEncryptionConfig.panEncryptor(dek).decrypt(vaultCard.getEncryptedPan());

            String pan = new String(panBytes, StandardCharsets.UTF_8);
            String expiry = vaultCard.getExpiryMonth() + "/" + vaultCard.getExpiryYear();

            PaymentProcessorRequest paymentProcessorRequest = PaymentProcessorRequest
                    .card(paymentId, pan, expiry, amount, methodDetails);

            PaymentProcessorResponse paymentProcessorResponse = paymentProcessorRouter
                    .routeToDedicatedPaymentProcessor(paymentProcessorRequest);

            log.info("Vault charge registered with token: {}*****", token.substring(0, 4));

            return paymentProcessorResponse;

        } catch (Exception e) {
            log.warn("Vault charge failed with token: {}*****", token.substring(0, 4));
            return new PaymentProcessorResponse.Failure("VAULT_CHARGE_FAILED", e.getMessage());

        } finally {
            if (panBytes != null) Arrays.fill(panBytes, (byte) 0);
        }
    }

    private CardType detectCardType(String pan) {

        if (pan == null || pan.isBlank()) return CardType.UNKNOWN;

        // Industry Standard: Strip all spaces and hyphens before running regex checks
        String sanitizedPan = pan.replaceAll("[\\s-]", "");

        // Loop through enum values to locate match pattern
        for (CardType type : CardType.values()) {
            if (type.getPattern() != null && type.getPattern().matcher(sanitizedPan).matches()) {
                return type;
            }
        }

        return CardType.UNKNOWN;
    }

}
