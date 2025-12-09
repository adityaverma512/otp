package com.example.otp.service;

import com.example.otp.config.SalesforceConfig;
import com.example.otp.exception.SalesforceServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
     * Request new JWT access token from Salesforce
     */
    private String requestNewToken() {
        try {
            String jwt = generateJWT();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            body.add("assertion", jwt);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    salesforceConfig.getAuth().getAuthenticationUri(),
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                cachedAccessToken = jsonNode.get("access_token").asText();

                // Set expiry time (subtract buffer for safety)
                int expirySeconds = salesforceConfig.getAuth().getTokenExpirySeconds();
                tokenExpiryTime = Instant.now().plusSeconds(expirySeconds);

                log.info("✅ Access token obtained successfully");
                return cachedAccessToken;
            }

            throw new SalesforceServiceException("Failed to obtain access token");

        } catch (Exception e) {
            log.error("❌ Failed to obtain Salesforce access token", e);
            throw new SalesforceServiceException("Authentication failed", e);
        }
    }

    /**
     * Generate JWT for Salesforce authentication
     */
    private String generateJWT() {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + TimeUnit.MINUTES.toMillis(5)); // 5 min validity

        return Jwts.builder()
                .setIssuer(salesforceConfig.getAuth().getClientId())
                .setSubject(salesforceConfig.getAuth().getClientId())
                .setAudience(salesforceConfig.getAuth().getAuthenticationUri())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(
                        SignatureAlgorithm.HS256,
                        salesforceConfig.getAuth().getJwtSigningSecret().getBytes()
                )
                .compact();
    }

    /**
     * Invalidate cached token (useful for testing or error recovery)
     */
    public void invalidateToken() {
        log.info("🔄 Invalidating cached access token");
        cachedAccessToken = null;
        tokenExpiryTime = null;
    }
}