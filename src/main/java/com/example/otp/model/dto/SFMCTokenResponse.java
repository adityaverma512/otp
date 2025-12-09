package com.example.otp.model.dto;

import lombok.Data;

@Data
public class SFMCTokenResponse {
    private String access_token;
    private String token_type;
    private int expires_in;
}
