package com.example.otp.client;

import com.example.otp.model.dto.OtpNotificationDto;
import com.example.otp.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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
    private NotificationService notificationService;

    public AzureServiceBusClient(ObjectMapper objectMapper,
                                 SalesforceClient salesforceClient,
                                 @Lazy NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.salesforceClient = salesforceClient;
        this.notificationService = notificationService;
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
            notificationService.updateNotificationStatus(
                    notification.getCorrelationId(),
                    "FAILED"
            );
        }
    }

    /**
     * Process message - Try once, if fails mark as FAILED
     */
    private void processMessage(OtpNotificationDto notification) {
        String correlationId = notification.getCorrelationId();

        try {
            log.info("🔄 [Message Consumer] Processing message");
            log.info("   Correlation ID: {}", correlationId);

            // Update status to PROCESSING
            notificationService.updateNotificationStatus(correlationId, "PROCESSING");

            // Call Salesforce (with circuit breaker protection)
            salesforceClient.sendOtp(notification);

            // SUCCESS
            notificationService.updateNotificationStatus(correlationId, "SENT");
            log.info("✅ [Message Consumer] OTP sent successfully");

        } catch (Exception e) {
            // FAILED - No retry, just mark as failed
            log.error("❌ [Message Consumer] Failed to send OTP: {}", e.getMessage());
            log.error("   Correlation ID: {}", correlationId);
            log.error("   User should request a new OTP");

            // Update status to FAILED
            notificationService.updateNotificationStatus(correlationId, "FAILED");
        }
    }

    /**
     * Real Azure Service Bus integration (to be implemented)
     */
    private void publishToRealServiceBus(OtpNotificationDto notification) {
        throw new UnsupportedOperationException("Real Azure Service Bus not yet implemented");
    }
}