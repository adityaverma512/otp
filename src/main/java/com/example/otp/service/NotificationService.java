package com.example.otp.service;

import com.example.otp.client.AzureServiceBusClient;
import com.example.otp.config.OtpConfig;
import com.example.otp.model.dto.OtpNotificationDto;
import com.example.otp.model.dto.inDto.OtpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final AzureServiceBusClient serviceBusClient;
    private final OtpConfig otpConfig;

    public NotificationService(AzureServiceBusClient serviceBusClient, OtpConfig otpConfig) {
        this.serviceBusClient = serviceBusClient;
        this.otpConfig = otpConfig;
    }

    /**
     * Send OTP notification asynchronously via Azure Service Bus
     * Fire and forget - no status tracking
     */
    public void sendOtpNotification(OtpRequest request, String identifier, String otp) {
        String correlationId = UUID.randomUUID().toString();

        log.info("📧 Preparing OTP notification - Correlation ID: {}", correlationId);

        OtpNotificationDto notification = OtpNotificationDto.builder()
                .correlationId(correlationId)
                .channel(request.getChannel())
                .identifier(identifier)
                .otp(otp)
                .firstName(request.getApplicantFirstName())
                .lastName(request.getApplicantLastName())
                .locale(request.getLocale() != null ? request.getLocale() : otpConfig.getDefaultLocale())
                .origSystem(otpConfig.getOrigSystem())
                .timestamp(Instant.now())
                .build();

        log.info("📤 Publishing OTP notification to Azure Service Bus");
        log.debug("Notification details: Channel={}, Identifier={}, Correlation={}",
                notification.getChannel(), notification.getIdentifier(), correlationId);

        // Publish message asynchronously (fire and forget)
        serviceBusClient.publishMessage(notification);

        log.info("✅ OTP notification published successfully (async)");
    }
}