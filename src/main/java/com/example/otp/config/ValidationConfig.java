package com.example.otp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "validation")
@Data
public class ValidationConfig {

    private EmailValidation email;
    private PhoneValidation phone;
    private OtpValidation otp;
    private NameValidation name;
    private SanitizationValidation sanitization;

    @Data
    public static class EmailValidation {
        private String pattern;
        private Integer maxLength;
    }

    @Data
    public static class PhoneValidation {
        private String pattern;
        private Integer minLength;
        private Integer maxLength;
    }

    @Data
    public static class OtpValidation {
        private String pattern;
        private Integer length;
    }

    @Data
    public static class NameValidation {
        private String pattern;
        private Integer maxLength;
    }
    @Data
    public static class SanitizationValidation {
        private String dangerousCharsPattern;
    }
}