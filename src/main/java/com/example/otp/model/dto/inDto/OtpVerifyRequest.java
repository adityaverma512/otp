package com.example.otp.model.dto.inDto;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String identifier;
    private String otp;
}