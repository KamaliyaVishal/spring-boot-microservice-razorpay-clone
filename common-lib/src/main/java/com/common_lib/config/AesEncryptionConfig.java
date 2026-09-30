package com.common_lib.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class AesEncryptionConfig {

    public BytesEncryptor masterKeyEncryptor(String masterKey, Integer keyLength) {
        byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
        SecretKeySpec masterDecKey = new SecretKeySpec(masterKeyBytes, "AES");

        // Initialize using the builder pattern API
        return AesGcmBytesEncryptor.withSecretKey(masterDecKey)
                .ivGenerator(KeyGenerators.secureRandom(keyLength))
                // Note: Spring's GCM default is usually 16 bytes, ensure compatibility if migrating data
                .build();
    }
}
