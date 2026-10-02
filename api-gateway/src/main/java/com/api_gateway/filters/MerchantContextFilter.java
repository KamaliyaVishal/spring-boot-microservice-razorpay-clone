package com.api_gateway.filters;


import com.common_lib.context.MerchantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Configuration
@RequiredArgsConstructor
// Executes 2nd: Runs after RequestContextFilter to capture basic inbound headers before authentication occurs
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MerchantContextFilter extends OncePerRequestFilter {

    public static final String MERCHANT_ID_HEADER = "X-Merchant-Id";
    public static final String KEY_ID_HEADER = "X-Key-Id";
    private final MerchantContext merchantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String merchantIdHeader = request.getHeader(MERCHANT_ID_HEADER);
            if (merchantIdHeader != null && !merchantIdHeader.isBlank()) {
                merchantContext.setMerchantId(UUID.fromString(merchantIdHeader));
            }

            String keyId = request.getHeader(KEY_ID_HEADER);
            if (keyId != null && !keyId.isBlank()) {
                merchantContext.setKeyId(keyId);
            }

            filterChain.doFilter(request, response);

        } finally {
            merchantContext.clear();
        }
    }
}