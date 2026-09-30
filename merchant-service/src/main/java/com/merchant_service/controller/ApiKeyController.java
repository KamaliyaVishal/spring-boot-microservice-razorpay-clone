package com.merchant_service.controller;

import com.common_lib.context.MerchantContext;
import com.merchant_service.dto.request.CreateApiKeyRequest;
import com.merchant_service.dto.response.ApiKeyResponse;
import com.merchant_service.dto.response.CreateApiKeyResponse;
import com.merchant_service.dto.response.DeleteResponse;
import com.merchant_service.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<CreateApiKeyResponse> create(@RequestBody @Valid CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.create(merchantContext.getMerchantId(), request));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> fetchAllApiKeys() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiKeyService.fetchAllApiKeys(merchantContext.getMerchantId()));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<DeleteResponse> revokeApiKey(@PathVariable String keyId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiKeyService.revokeApiKeyByMerchantId(merchantContext.getMerchantId(), keyId));
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<CreateApiKeyResponse> rotateApiKey(@PathVariable String keyId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.rotateApiKeyByMerchantId(merchantContext.getMerchantId(), keyId));
    }


}
