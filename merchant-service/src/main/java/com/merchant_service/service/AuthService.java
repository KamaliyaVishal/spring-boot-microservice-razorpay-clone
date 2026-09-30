package com.merchant_service.service;

import com.razorpay.merchant.dto.request.LoginRequest;
import com.razorpay.merchant.dto.request.MerchantRequest;
import com.razorpay.merchant.dto.response.LoginResponse;
import com.razorpay.merchant.dto.response.MerchantResponse;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthService {

    MerchantResponse signUpMerchant(MerchantRequest request);

    LoginResponse loginMerchant(LoginRequest request);
}
