package com.merchant_service.controller;

import com.merchant_service.dto.request.LoginRequest;
import com.merchant_service.dto.request.MerchantRequest;
import com.merchant_service.dto.response.LoginResponse;
import com.merchant_service.dto.response.MerchantResponse;
import com.merchant_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    ResponseEntity<MerchantResponse> signUpMerchant(@RequestBody @Valid MerchantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signUpMerchant(request));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> loginMerchant(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.loginMerchant(request));
    }

}
