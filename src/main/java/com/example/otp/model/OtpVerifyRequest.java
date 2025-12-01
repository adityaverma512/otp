package com.example.otp.model;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String identifier;
    private String otp;
}