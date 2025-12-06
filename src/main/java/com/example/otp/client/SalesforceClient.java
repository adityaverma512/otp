package com.example.otp.client;

import com.example.otp.config.SalesforceConfig;
import com.example.otp.exception.SalesforceServiceException;
import com.example.otp.model.dto.OtpNotificationDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
     * Send OTP notification to Salesforce with Circuit Breaker protection
     * NO RETRY - Fails immediately when circuit is OPEN
     */
    @CircuitBreaker(name = "salesforceClient", fallbackMethod = "sendOtpFallback")
    public void sendOtp(OtpNotificationDto notification) {
        log.info("🔵 [Salesforce] Attempting to send OTP");
        log.info("   Correlation ID: {}", notification.getCorrelationId());
        log.info("   Identifier: {}", notification.getIdentifier());

        if (salesforceConfig.getSimulation().getEnabled()) {
            simulateSalesforceCall(notification);
        } else {
            sendToRealSalesforce(notification);
        }

        log.info("✅ [Salesforce] OTP sent successfully");
    }

    /**
     * Fallback method - Called when circuit is OPEN or call fails
     * NO RETRY - Just log and throw exception
     */
    private void sendOtpFallback(OtpNotificationDto notification, Exception ex) {
        // Check if circuit breaker is open
        if (ex instanceof CallNotPermittedException) {
            log.error("🔴 ========================================");
            log.error("🔴 CIRCUIT BREAKER IS OPEN");
            log.error("🔴 SALESFORCE IS DOWN - NOT ATTEMPTING");
            log.error("🔴 Correlation ID: {}", notification.getCorrelationId());
            log.error("🔴 ========================================");

            throw new SalesforceServiceException(
                    "Salesforce service is temporarily unavailable. Circuit breaker is OPEN. Please try again later."
            );
        }

        // Other failures
        log.error("❌ [Salesforce] Failed to send OTP");
        log.error("   Correlation ID: {}", notification.getCorrelationId());
        log.error("   Error: {}", ex.getMessage());

        throw new SalesforceServiceException(
                "Failed to send OTP notification. Please try again.",
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
            log.debug("⏳ [Simulation] Network delay: {}ms", delay);
            Thread.sleep(delay);

            // Simulate random failures based on failure rate
            double failureRate = salesforceConfig.getSimulation().getFailureRate();
            if (random.nextDouble() < failureRate) {
                log.warn("⚠️ [Simulation] Forcing failure (rate: {}%)", failureRate * 100);
                throw new RuntimeException("Simulated Salesforce failure");
            }

            // Simulate timeout scenario
            if (delay > salesforceConfig.getSimulation().getTimeoutMs()) {
                log.warn("⚠️ [Simulation] Timeout detected");
                throw new TimeoutException("Simulated timeout");
            }

            log.info("✅ [Simulation] OTP sent successfully");
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
        throw new UnsupportedOperationException("Real Salesforce integration not yet implemented");
    }
}