package com.example.otp.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, SalesforceConfig salesforceConfig) {
        return builder
                .setConnectTimeout(Duration.ofMillis(salesforceConfig.getApi().getConnectionTimeout()))
                .setReadTimeout(Duration.ofMillis(salesforceConfig.getApi().getTimeout()))
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
    }
}