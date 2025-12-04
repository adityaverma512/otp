package com.example.otp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpNotificationDto {

    private String correlationId;
    private String channel;              // SMS or EMAIL
    private String identifier;           // Phone or Email
    private String otp;                  // Plain OTP to send
    private String firstName;
    private String lastName;
    private String locale;
    private Instant timestamp;
    private String origSystem;
}