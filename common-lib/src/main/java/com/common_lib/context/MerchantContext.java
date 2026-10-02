package com.common_lib.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Getter
@Setter
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MerchantContext {
    private UUID merchantId;
    private String keyId;

    public void clear() {
        this.merchantId = null; // or threadLocal.remove(); if using ThreadLocal
        this.keyId = null;
    }
}
