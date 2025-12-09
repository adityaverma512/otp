package com.example.otp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "salesforce")
@Data
public class SalesforceConfig {

    private SimulationConfig simulation;
    private AuthConfig auth;
    private ApiConfig api;
    private OtpConfig otp;

    @Data
    public static class SimulationConfig {
        private Boolean enabled;
        private Double failureRate;
        private Integer delayMs;
        private Integer timeoutMs;
    }

    @Data
    public static class AuthConfig {
        private String jwtSigningSecret;
        private String clientId;
        private String clientSecret;
        private String authenticationUri;
        private Integer tokenExpirySeconds;
    }

    @Data
    public static class ApiConfig {
        private String restUri;
        private String soapUri;
        private Integer timeout;
        private Integer connectionTimeout;
    }

    @Data
    public static class OtpConfig {
        private String smsApiKey;
        private String emailApiKey;
    }
}