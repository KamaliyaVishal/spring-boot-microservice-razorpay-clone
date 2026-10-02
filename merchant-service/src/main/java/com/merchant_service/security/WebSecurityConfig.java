package com.merchant_service.security;

import com.common_lib.idempotency.IdempotencyFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private static final String[] PUBLIC_ROUTES = {"/api/v1/auth/signup/**", "/api/v1/auth/login/**", "/api/v1/webhook/**"};
    private static final String[] JWT_ROUTES = {"/api/v1/auth/**", "/api/v1/merchants/**", "/api/v1/admin/**", "/api/actuator/**", "/api/v1/webhook/**"};
    private static final String[] API_KEY_ROUTES = {"/api/v1/orders/**", "/api/v1/payment/**", "/api/v1/vault/**"};

    private final IdempotencyFilter idempotencyFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
