package com.example.otp.client;

import com.example.otp.model.dto.OtpNotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AzureServiceBusClient {

    @Value("${azure.servicebus.simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${azure.servicebus.simulation.queue-name:otp-notification-queue}")
    private String queueName;

    private final ObjectMapper objectMapper;
    private final SalesforceClient salesforceClient;

    public AzureServiceBusClient(ObjectMapper objectMapper,
                                 SalesforceClient salesforceClient) {
        this.objectMapper = objectMapper;
        this.salesforceClient = salesforceClient;
    }

    /**
     * Publish message to Azure Service Bus (async)
     */
    @Async("otpNotificationExecutor")
    public void publishMessage(OtpNotificationDto notification) {
        if (simulationEnabled) {
            publishToSimulatedQueue(notification);
        } else {
            publishToRealServiceBus(notification);
        }
    }

    /**
     * Simulated message publishing - Simple version
     * NO RETRY, NO DLQ - Single attempt only
     */
    private void publishToSimulatedQueue(OtpNotificationDto notification) {
        try {
            log.info("📨 [Azure Service Bus] Publishing message to queue: {}", queueName);
            log.debug("Message: {}", objectMapper.writeValueAsString(notification));

            log.info("✅ [Azure Service Bus] Message published successfully");

            // Process the message immediately (one attempt only)
            processMessage(notification);

        } catch (Exception e) {
            log.error("❌ [Azure Service Bus] Failed to publish message", e);
            log.error("   Correlation ID: {}", notification.getCorrelationId());
            log.error("   Message will NOT be retried - User must request new OTP");
        }
    }

    /**
     * Process message - Try once, if fails just log error
     * Circuit breaker will handle Salesforce failures
     */
    private void processMessage(OtpNotificationDto notification) {
        String correlationId = notification.getCorrelationId();

        try {
            log.info("🔄 [Message Consumer] Processing message");
            log.info("   Correlation ID: {}", correlationId);

            // Call Salesforce (with circuit breaker protection)
            salesforceClient.sendOtp(notification);

            log.info("✅ [Message Consumer] OTP sent successfully");

        } catch (Exception e) {
            log.error("❌ [Message Consumer] Failed to send OTP");
            log.error("   Correlation ID: {}", correlationId);
            log.error("   Error: {}", e.getMessage());
            log.error("   User must request a new OTP");
        }
    }

    /**
     * Real Azure Service Bus integration (to be implemented)
     */
    private void publishToRealServiceBus(OtpNotificationDto notification) {
        throw new UnsupportedOperationException("Real Azure Service Bus not yet implemented");
    }
}