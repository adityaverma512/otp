package com.example.otp.service;

import com.example.otp.config.SalesforceConfig;
import com.example.otp.exception.SalesforceServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SalesforceAuthService {

    private final SalesforceConfig salesforceConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedAccessToken;
    private Instant tokenExpiryTime;

    public SalesforceAuthService(SalesforceConfig salesforceConfig,
                                 RestTemplate restTemplate,
                                 ObjectMapper objectMapper) {
        this.salesforceConfig = salesforceConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get access token (cached or new)
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Using cached access token");
            return cachedAccessToken;
        }

        log.info("🔑 Requesting new Salesforce access token");
        return requestNewToken();
    }

    /**
     * Check if cached token is still valid
     */
    private boolean isTokenValid() {
        if (cachedAccessToken == null || tokenExpiryTime == null) {
            return false;
        }
        // Token valid if expires in more than 60 seconds
        return Instant.now().isBefore(tokenExpiryTime.minusSeconds(60));
    }

    /**
     * Request new Access Token using Client Credentials Flow (v2)
     * Refactored to match the working reference implementation.
     */
    private String requestNewToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build JSON Body equivalent to the working example's SFMCTokenRequest
            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("client_id", salesforceConfig.getAuth().getClientId());
            body.put("client_secret", salesforceConfig.getAuth().getClientSecret());
            // Optional: If your setup requires account_id, uncomment below
            // body.put("account_id", "YOUR_BUSINESS_UNIT_ID");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            // Ensure URL ends with /v2/token
            String authUrl = salesforceConfig.getAuth().getAuthenticationUri();
            if (!authUrl.endsWith("/v2/token")) {
                authUrl = authUrl.replaceAll("/+$", "") + "/v2/token";
            }

            ResponseEntity<String> response = restTemplate.postForEntity(
                    authUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                cachedAccessToken = jsonNode.get("access_token").asText();

                // Get expiry from response or fallback to config
                int expirySeconds = jsonNode.has("expires_in")
                        ? jsonNode.get("expires_in").asInt()
                        : salesforceConfig.getAuth().getTokenExpirySeconds();

                tokenExpiryTime = Instant.now().plusSeconds(expirySeconds);

                log.info("✅ Access token obtained successfully");
                return cachedAccessToken;
            }

            throw new SalesforceServiceException("Failed to obtain access token: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("❌ Failed to obtain Salesforce access token", e);
            throw new SalesforceServiceException("Authentication failed", e);
        }
    }

    /**
     * Invalidate cached token
     */
    public void invalidateToken() {
        log.info("🔄 Invalidating cached access token");
        cachedAccessToken = null;
        tokenExpiryTime = null;
    }
}