package com.common_lib;

import com.common_lib.audit.AuditorAwareImpl;
import com.common_lib.config.AesEncryptionConfig;
import com.common_lib.config.KafkaProperties;
import com.common_lib.context.MerchantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.encrypt.BytesEncryptor;

@AutoConfiguration
@EnableConfigurationProperties(KafkaProperties.class)
public class CommonLibAutoConfigurations {

    @Bean("AuditorAwareImpl")
    public AuditorAware<String> auditorAwareImpl(MerchantContext merchantContext) {
        return new AuditorAwareImpl(merchantContext);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "vault.master-key")
    public BytesEncryptor masterKeyEncryptor(@Value("${vault.master-key}") String masterKey, @Value("${vault.master-key}") Integer keyLength) {
        return new AesEncryptionConfig().masterKeyEncryptor(masterKey, keyLength);
    }


}
