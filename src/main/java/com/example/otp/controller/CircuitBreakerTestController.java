package com.example.otp.controller;

import com.example.otp.config.SalesforceConfig;
import com.example.otp.model.dto.outDto.ApiResponseOutDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing and monitoring Circuit Breaker
 * Remove or secure this in production!
 */
@RestController
@RequestMapping("/circuit-breaker")
@Slf4j
public class CircuitBreakerTestController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final SalesforceConfig salesforceConfig;

    public CircuitBreakerTestController(CircuitBreakerRegistry circuitBreakerRegistry,
                                        SalesforceConfig salesforceConfig) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.salesforceConfig = salesforceConfig;
    }

    /**
     * Get circuit breaker status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponseOutDto<Map<String, Object>>> getStatus() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("salesforceClient");

        Map<String, Object> status = new HashMap<>();
        status.put("state", circuitBreaker.getState().name());
        status.put("failureRate", circuitBreaker.getMetrics().getFailureRate() + "%");
        status.put("slowCallRate", circuitBreaker.getMetrics().getSlowCallRate() + "%");
        status.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        status.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        status.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        status.put("numberOfSlowCalls", circuitBreaker.getMetrics().getNumberOfSlowCalls());

        ApiResponseOutDto<Map<String, Object>> response = ApiResponseOutDto.<Map<String, Object>>builder()
                .status("SUCCESS")
                .message("Circuit breaker status retrieved")
                .data(status)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Manually transition circuit breaker to CLOSED state
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponseOutDto<String>> resetCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("salesforceClient");
        circuitBreaker.transitionToClosedState();

        log.info("🔄 Circuit breaker manually reset to CLOSED state");

        ApiResponseOutDto<String> response = ApiResponseOutDto.<String>builder()
                .status("SUCCESS")
                .message("Circuit breaker reset to CLOSED state")
                .data(null)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Update Salesforce simulation failure rate (for testing)
     */
    @PostMapping("/simulate-failure-rate")
    public ResponseEntity<ApiResponseOutDto<Map<String, Object>>> setFailureRate(
            @RequestParam double rate) {

        if (rate < 0.0 || rate > 1.0) {
            return ResponseEntity.badRequest().body(
                    ApiResponseOutDto.<Map<String, Object>>builder()
                            .status("ERROR")
                            .message("Failure rate must be between 0.0 and 1.0")
                            .data(null)
                            .timestamp(Instant.now())
                            .build()
            );
        }

        salesforceConfig.getSimulation().setFailureRate(rate);

        log.info("🎯 Salesforce simulation failure rate set to: {}%", rate * 100);

        Map<String, Object> data = new HashMap<>();
        data.put("failureRate", rate);
        data.put("failurePercentage", (rate * 100) + "%");

        ApiResponseOutDto<Map<String, Object>> response = ApiResponseOutDto.<Map<String, Object>>builder()
                .status("SUCCESS")
                .message("Failure rate updated successfully")
                .data(data)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get current simulation settings
     */
    @GetMapping("/simulation-settings")
    public ResponseEntity<ApiResponseOutDto<Map<String, Object>>> getSimulationSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", salesforceConfig.getSimulation().getEnabled());
        settings.put("failureRate", salesforceConfig.getSimulation().getFailureRate());
        settings.put("delayMs", salesforceConfig.getSimulation().getDelayMs());
        settings.put("timeoutMs", salesforceConfig.getSimulation().getTimeoutMs());

        ApiResponseOutDto<Map<String, Object>> response = ApiResponseOutDto.<Map<String, Object>>builder()
                .status("SUCCESS")
                .message("Simulation settings retrieved")
                .data(settings)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }
}