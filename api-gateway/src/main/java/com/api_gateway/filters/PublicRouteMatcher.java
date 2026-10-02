package com.api_gateway.filters;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PublicRouteMatcher {

    private final SecurityRouteProperties securityRouteProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${app.security.public-routes:}")
    private List<String> publicRoutes;

    public boolean isPublic(String path) {
        return securityRouteProperties.getPublicRoutes().stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

}
