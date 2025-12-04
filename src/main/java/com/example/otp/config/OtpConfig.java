package com.example.otp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp")
@Data
public class OtpConfig {

    private Integer length;
    private Integer expirySeconds;
    private ResendConfig resend;
    private SecurityConfig security;
    private String origSystem;
    private String defaultLocale;

    @Data
    public static class ResendConfig {
        private Integer cooldownSeconds;
        private Boolean generateNewOtp;
    }

    @Data
    public static class SecurityConfig {
        private String hashAlgorithm;
        private Boolean useHashing;
    }
}