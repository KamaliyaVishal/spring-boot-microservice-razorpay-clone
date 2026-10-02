package com.common_lib;

import com.common_lib.audit.AuditorAwareImpl;
import com.common_lib.cache.ApiKeyCache;
import com.common_lib.cache.RedisApiKeyCache;
import com.common_lib.config.AesEncryptionConfig;
import com.common_lib.config.KafkaProperties;
import com.common_lib.context.MerchantContext;
import com.common_lib.hadler.GlobalExceptionHandler;
import com.common_lib.idempotency.IdempotencyFilter;
import com.common_lib.idempotency.IdempotencyStore;
import com.common_lib.idempotency.impl.RedisIdempotencyStore;
import com.common_lib.ratelimiter.RateLimiter;
import com.common_lib.ratelimiter.impl.FixedWindowRateLimiter;
import com.common_lib.ratelimiter.impl.SlidingWindowLuaLimiter;
import com.common_lib.ratelimiter.impl.SlidingWindowRateLimiter;
import com.common_lib.ratelimiter.impl.TokenBucketRateLimiter;
import com.common_lib.util.JwtUtil;
import com.common_lib.util.SignerUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(KafkaProperties.class)
public class CommonLibAutoConfigurations {

    @Bean("auditorAwareImpl")
    public AuditorAware<String> auditorAwareImpl(MerchantContext merchantContext) {
        return new AuditorAwareImpl(merchantContext);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "vault.master-key")
    public BytesEncryptor masterKeyEncryptor(@Value("${vault.master-key}") String masterKey,
                                             @Value("${vault.master-key}") Integer keyLength) {
        return new AesEncryptionConfig().masterKeyEncryptor(masterKey, keyLength);
    }

    @Bean
    @ConditionalOnProperty(name = "webhook.secret-encryption-key")
    public BytesEncryptor webhookSecretEncryptor(@Value("${webhook.secret-encryption-key}") String masterKey,
                                                 @Value("${vault.master-key}") Integer keyLength) {
        return new AesEncryptionConfig().masterKeyEncryptor(masterKey, keyLength);
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public MerchantContext merchantContext() {
        return new MerchantContext();
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public IdempotencyStore idempotencyStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisIdempotencyStore(stringRedisTemplate);
    }

    @Bean
    public IdempotencyFilter idempotencyFilter(MerchantContext merchantContext,
                                               IdempotencyStore idempotencyStore,
                                               @Qualifier("handlerExceptionResolver")
                                                   HandlerExceptionResolver handlerExceptionResolver) {
        return new IdempotencyFilter(merchantContext, idempotencyStore, handlerExceptionResolver);
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "fixed")
    public RateLimiter fixedWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        return new FixedWindowRateLimiter(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "sliding")
    public RateLimiter slidingWindowRateLimiter(StringRedisTemplate redis) {
        return new SlidingWindowRateLimiter(redis);
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "sliding-lua")
    public RateLimiter slidingWindowLuaLimiter(StringRedisTemplate redis) {
        return new SlidingWindowLuaLimiter(redis);
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "bucket")
    public RateLimiter tokenBucketRateLimiter(StringRedisTemplate redis) {
        return new TokenBucketRateLimiter(redis);
    }

    @Bean
    public SignerUtil signerUtil() {
        return new SignerUtil();
    }

    @Bean
    public JwtUtil jwtUtil(@Value("${app.jwt.secret-key}") String secretKey) {
        return new JwtUtil(secretKey);
    }

    @Bean
    public ApiKeyCache apiKeyCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisApiKeyCache(stringRedisTemplate, objectMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
