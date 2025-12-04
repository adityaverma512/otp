package com.example.otp.client;

import com.example.otp.config.SalesforceConfig;
import com.example.otp.exception.SalesforceServiceException;
import com.example.otp.model.dto.OtpNotificationDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class SalesforceClient {

    private final SalesforceConfig salesforceConfig;
    private final Random random = new Random();

    public SalesforceClient(SalesforceConfig salesforceConfig) {
        this.salesforceConfig = salesforceConfig;
    }

    /**
     * Send OTP notification to Salesforce
     * Circuit breaker protects against cascading failures
     */
    @CircuitBreaker(name = "salesforceClient", fallbackMethod = "sendOtpFallback")
    public void sendOtp(OtpNotificationDto notification) {
        log.info("📤 [CircuitBreaker] Attempting to send OTP via Salesforce for: {} (Correlation: {})",
                notification.getIdentifier(), notification.getCorrelationId());

        if (salesforceConfig.getSimulation().getEnabled()) {
            simulateSalesforceCall(notification);
        } else {
            // TODO: Implement actual Salesforce API call
            sendToRealSalesforce(notification);
        }

        log.info("✅ [CircuitBreaker] OTP sent successfully via Salesforce for: {}",
                notification.getIdentifier());
    }

    /**
     * Fallback method when circuit is OPEN or call fails
     */
    private void sendOtpFallback(OtpNotificationDto notification, Exception ex) {
        log.error("🔴 [CircuitBreaker FALLBACK] Salesforce unavailable. Correlation: {}. Error: {}",
                notification.getCorrelationId(), ex.getMessage());

        throw new SalesforceServiceException(
                "OTP service is temporarily unavailable. Please try again in a few moments.",
                ex
        );
    }

    /**
     * Simulate Salesforce behavior for testing
     */
    private void simulateSalesforceCall(OtpNotificationDto notification) {
        try {
            // Simulate network delay
            int delay = salesforceConfig.getSimulation().getDelayMs();
            log.debug("⏳ Simulating Salesforce delay: {}ms", delay);
            Thread.sleep(delay);

            // Simulate random failures based on failure rate
            double failureRate = salesforceConfig.getSimulation().getFailureRate();
            if (random.nextDouble() < failureRate) {
                log.warn("⚠️ [SIMULATION] Forcing failure (failure rate: {})", failureRate);
                throw new RuntimeException("Simulated Salesforce failure");
            }

            // Simulate timeout scenario
            if (delay > salesforceConfig.getSimulation().getTimeoutMs()) {
                log.warn("⚠️ [SIMULATION] Simulating timeout");
                throw new TimeoutException("Simulated timeout");
            }

            log.info("✅ [SIMULATION] OTP sent successfully via Salesforce simulator");
            log.info("   Channel: {} | Identifier: {} | Name: {} {}",
                    notification.getChannel(),
                    notification.getIdentifier(),
                    notification.getFirstName(),
                    notification.getLastName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SalesforceServiceException("Salesforce call interrupted", e);
        } catch (TimeoutException e) {
            throw new SalesforceServiceException("Salesforce call timed out", e);
        }
    }

    /**
     * Real Salesforce integration (to be implemented)
     */
    private void sendToRealSalesforce(OtpNotificationDto notification) {
        // TODO: Implement actual HTTP call to Salesforce
        // Example using RestTemplate or WebClient:
        //
        // ResponseEntity<String> response = restTemplate.postForEntity(
        //     salesforceConfig.getApi().getEndpoint(),
        //     notification,
        //     String.class
        // );

        throw new UnsupportedOperationException("Real Salesforce integration not yet implemented");
    }
}