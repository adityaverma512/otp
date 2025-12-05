package com.example.otp.model.dto.inDto;

import lombok.Data;

@Data
public class OtpRequest {

    private String channel;
    private String applicationMobileNumber;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicationEmailAddress;
    private String locale;
}