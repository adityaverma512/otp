package com.example.otp.model.dto.inDto;

import lombok.Data;

@Data
public class OtpVerifyRequest {

    private String channel;
    private String identifier;
    private String otp;
}