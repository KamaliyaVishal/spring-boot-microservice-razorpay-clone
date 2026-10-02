package com.api_gateway.security;

import com.common_lib.idempotency.IdempotencyFilter;
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

@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // Sets the filter execution order directly
public class IdempotencyApiFilter extends OncePerRequestFilter {

    private final IdempotencyFilter idempotencyFilter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Delegate the lifecycle control to the common library filter instance
        idempotencyFilter.doFilter(request, response, filterChain);
    }
}