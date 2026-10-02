package com.merchant_service.service.Impl;

import com.common_lib.enums.MerchantStatus;
import com.common_lib.enums.UserRole;
import com.common_lib.exception.BusinessRuleViolationException;
import com.common_lib.exception.DuplicateResourceException;
import com.common_lib.exception.ResourceNotFoundException;
import com.common_lib.util.JwtUtil;
import com.merchant_service.dto.request.LoginRequest;
import com.merchant_service.dto.request.MerchantRequest;
import com.merchant_service.dto.response.LoginResponse;
import com.merchant_service.dto.response.MerchantResponse;
import com.merchant_service.entity.AppUser;
import com.merchant_service.entity.Merchant;
import com.merchant_service.mapper.GlobalMerchantMapper;
import com.merchant_service.repository.AppUserRepository;
import com.merchant_service.repository.MerchantRepository;
import com.merchant_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final MerchantRepository merchantRepository;
    private final GlobalMerchantMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantResponse signUpMerchant(MerchantRequest request) {
        if (merchantRepository.existsByEmail(request.email()))
            throw new DuplicateResourceException("Merchant with Email already exists", "Email", request.email());

        //Save the merchant and user details to the database and return the response
        Merchant merchant = mapper.fromMerchantRequest(request);
        merchant.setStatus(MerchantStatus.PENDING_KYC);

        merchantRepository.save(merchant);

        //Create user from signup details
        AppUser appUser = AppUser.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .merchant(merchant)
                .build();

        appUserRepository.save(appUser);

        return mapper.toMerchantResponse(merchant);

    }

    @Override
    public LoginResponse loginMerchant(LoginRequest request) {

        AppUser appUser = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        if (!passwordEncoder.matches(request.password(), appUser.getPasswordHash())) {
            throw new BusinessRuleViolationException("INVALID_CREDENTIALS", "Invalid email or password", request.password());
        }

        String accessToken = jwtUtil.generateAccessToken(request.email(), appUser.getMerchant().getId(),
                appUser.getRole().toString());

        return new LoginResponse(accessToken);
    }
}
