package com.example.otp.client;

import com.example.otp.config.SalesforceConfig;
import com.example.otp.constants.OtpConstants;
import com.example.otp.exception.SalesforceServiceException;
import com.example.otp.model.dto.OtpNotificationDto;
import com.example.otp.model.dto.SalesforceOtpRequest;
import com.example.otp.service.SFMCAuthService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class SalesforceClient {

    private final SalesforceConfig salesforceConfig;
    private final SFMCAuthService authService;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    public SalesforceClient(SalesforceConfig salesforceConfig,
                            SFMCAuthService authService,
                            RestTemplate restTemplate) {
        this.salesforceConfig = salesforceConfig;
        this.authService = authService;
        this.restTemplate = restTemplate;
    }

    /**
     * Send OTP notification to Salesforce with Circuit Breaker protection
     */
    @CircuitBreaker(name = "salesforceClient", fallbackMethod = "sendOtpFallback")
    public void sendOtp(OtpNotificationDto notification) {
        log.info("🔵 [Salesforce] Attempting to send OTP");
        log.info("   Correlation ID: {}", notification.getCorrelationId());
        log.info("   Channel: {} | Identifier: {}", notification.getChannel(), notification.getIdentifier());

        if (salesforceConfig.getSimulation().getEnabled()) {
            simulateSalesforceCall(notification);
        } else {
            sendToRealSalesforce(notification);
        }

        log.info("✅ [Salesforce] OTP sent successfully");
    }

    /**
     * Fallback method - Called when circuit is OPEN or call fails
     */
    private void sendOtpFallback(OtpNotificationDto notification, Exception ex) {
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

        log.error("❌ [Salesforce] Failed to send OTP");
        log.error("   Correlation ID: {}", notification.getCorrelationId());
        log.error("   Error: {}", ex.getMessage());

        throw new SalesforceServiceException(
                "Failed to send OTP notification. Please try again.",
                ex
        );
    }

    /**
     * Real Salesforce integration
     */
    private void sendToRealSalesforce(OtpNotificationDto notification) {
        try {
            // Get access token
            String accessToken = authService.getAccessToken();

            // Build request
            SalesforceOtpRequest sfRequest = buildSalesforceRequest(notification);

            // Determine endpoint and API key based on channel
            String endpoint = salesforceConfig.getApi().getRestUri();
            String apiKey = OtpConstants.CHANNEL_SMS.equals(notification.getChannel())
                    ? salesforceConfig.getOtp().getSmsApiKey()
                    : salesforceConfig.getOtp().getEmailApiKey();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("X-API-Key", apiKey);

            HttpEntity<SalesforceOtpRequest> request = new HttpEntity<>(sfRequest, headers);

            log.debug("📤 Sending OTP to Salesforce: {}", endpoint);

            // Send request
            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [Salesforce] OTP sent successfully");
                log.info("   Channel: {} | Identifier: {} | Name: {} {}",
                        notification.getChannel(),
                        notification.getIdentifier(),
                        notification.getFirstName(),
                        notification.getLastName());
            } else {
                throw new SalesforceServiceException(
                        "Salesforce returned non-success status: " + response.getStatusCode()
                );
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ HTTP error calling Salesforce: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            // If unauthorized, invalidate token and retry once
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("⚠️ Unauthorized - invalidating token and retrying once");
                //authService.invalidateToken();
            }

            throw new SalesforceServiceException("Salesforce API call failed", e);

        } catch (Exception e) {
            log.error("❌ Unexpected error calling Salesforce", e);
            throw new SalesforceServiceException("Salesforce call failed", e);
        }
    }

    /**
     * Build Salesforce request from notification
     */
    private SalesforceOtpRequest buildSalesforceRequest(OtpNotificationDto notification) {
        return SalesforceOtpRequest.builder()
                .applicationMobileNumber(
                        OtpConstants.CHANNEL_SMS.equals(notification.getChannel())
                                ? notification.getIdentifier()
                                : null
                )
                .applicationEmailAddress(
                        OtpConstants.CHANNEL_EMAIL.equals(notification.getChannel())
                                ? notification.getIdentifier()
                                : null
                )
                .applicantFirstName(notification.getFirstName())
                .applicantLastName(notification.getLastName())
                .locale(notification.getLocale())
                .otp(notification.getOtp())
                .origSystem(notification.getOrigSystem())
                .build();
    }

    /**
     * Simulate Salesforce behavior for testing
     */
    private void simulateSalesforceCall(OtpNotificationDto notification) {
        try {
            int delay = salesforceConfig.getSimulation().getDelayMs();
            log.debug("⏳ [Simulation] Network delay: {}ms", delay);
            Thread.sleep(delay);

            double failureRate = salesforceConfig.getSimulation().getFailureRate();
            if (random.nextDouble() < failureRate) {
                log.warn("⚠️ [Simulation] Forcing failure (rate: {}%)", failureRate * 100);
                throw new RuntimeException("Simulated Salesforce failure");
            }

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
}