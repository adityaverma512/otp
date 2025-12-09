package com.example.otp.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesforceOtpRequest {

    @JsonProperty("ApplicationMobileNumber")
    private String applicationMobileNumber;

    @JsonProperty("ApplicantFirstName")
    private String applicantFirstName;

    @JsonProperty("ApplicantLastName")
    private String applicantLastName;

    @JsonProperty("ApplicationEmailAddress")
    private String applicationEmailAddress;

    @JsonProperty("Locale")
    private String locale;

    @JsonProperty("OTP")
    private String otp;

    @JsonProperty("OrigSystem")
    private String origSystem;
}