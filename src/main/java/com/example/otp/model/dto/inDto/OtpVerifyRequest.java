package com.example.otp.model.dto.inDto;

import lombok.Data;

@Data
public class OtpVerifyRequest {

    private String channel;                      // "SMS" or "EMAIL"
    private String identifier;                   // Phone or Email based on channel
    private String otp;                          // 6-digit OTP
}