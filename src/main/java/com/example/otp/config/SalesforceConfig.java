package com.example.otp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "salesforce")
@Data
public class SalesforceConfig {

    private SimulationConfig simulation;
    private ApiConfig api;

    @Data
    public static class SimulationConfig {
        private Boolean enabled;
        private Double failureRate;  // 0.0 to 1.0
        private Integer delayMs;
        private Integer timeoutMs;
    }

    @Data
    public static class ApiConfig {
        private String endpoint;
        private Integer timeout;
    }
}