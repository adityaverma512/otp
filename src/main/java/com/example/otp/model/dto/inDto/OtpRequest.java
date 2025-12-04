package com.example.otp.model.dto.inDto;

import lombok.Data;

@Data
public class OtpRequest {

    private String channel;                      // "SMS" or "EMAIL"
    private String applicationMobileNumber;      // Required if channel=SMS
    private String applicantFirstName;           // Required
    private String applicantLastName;            // Required
    private String applicationEmailAddress;      // Required if channel=EMAIL
    private String locale;                       // Optional (default from config)
}