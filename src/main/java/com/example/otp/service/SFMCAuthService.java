package com.example.otp.service;


import com.example.otp.model.dto.SFMCTokenRequest;
import com.example.otp.model.dto.SFMCTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SFMCAuthService {

    @Value("${sfmc.client.id}")
    private String clientId;

    @Value("${sfmc.client.secret}")
    private String clientSecret;

    @Value("${sfmc.token.url}")
    private String tokenUrl;

    @Value("${sfmc.auth.granttype}")
    private String grantType;

    private final RestTemplate restTemplate;

    public SFMCAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAccessToken() {

        SFMCTokenRequest request =  SFMCTokenRequest.builder()
                .client_id(clientId)
                .client_secret(clientSecret)
                .grant_type(grantType)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SFMCTokenRequest> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<SFMCTokenResponse> response =
                restTemplate.exchange(tokenUrl, HttpMethod.POST, httpEntity, SFMCTokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to fetch token from SFMC");
        }

        return response.getBody().getAccess_token();
    }
}
