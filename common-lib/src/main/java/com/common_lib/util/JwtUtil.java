package com.common_lib.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {

    private final String secretKey;

    public JwtUtil(String secretKey) {
        this.secretKey = secretKey;
    }

    public String generateAccessToken(String email, UUID merchantId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .signWith(getSecretKey())
                .subject(email)
                .claim("merchantId", merchantId)
                .claim("role", role)
                .expiration(Date.from(now.plusSeconds(60 * 100)))
                .compact();
    }

    public Claims verify(String accessToken) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String extractMerchantId(Claims claims) {
        return claims.get("merchant_id", String.class);
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
