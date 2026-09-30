package com.merchant_service.mapper;


import com.merchant_service.dto.request.MerchantRequest;
import com.merchant_service.dto.response.ApiKeyResponse;
import com.merchant_service.dto.response.CreateApiKeyResponse;
import com.merchant_service.dto.response.MerchantResponse;
import com.merchant_service.dto.response.WebhookConfigResponse;
import com.merchant_service.entity.ApiKey;
import com.merchant_service.entity.Merchant;
import com.merchant_service.entity.MerchantWebhookConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GlobalMerchantMapper {

    CreateApiKeyResponse toCreateApiKeyResponse(ApiKey apiKey);

    List<ApiKeyResponse> toApiKeyResponseList(List<ApiKey> apiKeys);

    MerchantResponse toMerchantResponse(Merchant merchant);

    Merchant fromMerchantRequest(MerchantRequest merchantRequest);

    @Mapping(target = "webhookSecret", source = "rawSecret")
    WebhookConfigResponse toResponse(MerchantWebhookConfig merchantWebhookConfig, String rawSecret);

}
