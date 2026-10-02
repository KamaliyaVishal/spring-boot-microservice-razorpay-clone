package com.api_gateway.security.jwt;

import com.api_gateway.security.exception.GatewayAuthenticationException;
import com.common_lib.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthHandler {

    private final JwtUtil jwtUtil;

    public Map<String, String> authenticate(String token) {
        Claims claims;
        try {
            claims = jwtUtil.verify(token);
        } catch (Exception e) {
            throw new GatewayAuthenticationException("Invalid or expired token");
        }

        return Map.of(
                "X-Merchant-Id", jwtUtil.extractMerchantId(claims),
                "X-User-Role", jwtUtil.extractRole(claims)
        );
    }
}
