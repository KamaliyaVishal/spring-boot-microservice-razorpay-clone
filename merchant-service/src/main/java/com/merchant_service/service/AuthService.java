package com.merchant_service.service;

import com.merchant_service.dto.request.LoginRequest;
import com.merchant_service.dto.request.MerchantRequest;
import com.merchant_service.dto.response.LoginResponse;
import com.merchant_service.dto.response.MerchantResponse;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthService {

    MerchantResponse signUpMerchant(MerchantRequest request);

    LoginResponse loginMerchant(LoginRequest request);
}
