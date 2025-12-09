package com.example.otp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SFMCTokenRequest {
    private String grant_type;
    private String client_id;
    private String client_secret;
}
