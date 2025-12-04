package com.example.otp.controller;

import com.example.otp.model.dto.inDto.OtpRequest;
import com.example.otp.model.dto.inDto.OtpVerifyRequest;
import com.example.otp.model.dto.outDto.ApiResponseOutDto;
import com.example.otp.service.OtpService;
import com.example.otp.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;
    private final ValidationService validationService;

    public OtpController(OtpService otpService, ValidationService validationService) {
        this.otpService = otpService;
        this.validationService = validationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponseOutDto<Map<String, String>>> generate(@RequestBody OtpRequest request) {
        // Validate and sanitize request
        validationService.validateGenerateRequest(request);

        // Get identifier based on channel
        String identifier = validationService.getIdentifier(request);

        // Generate OTP
        String otp = otpService.generate(identifier);

        ApiResponseOutDto<Map<String, String>> response = ApiResponseOutDto.<Map<String, String>>builder()
                .status("SUCCESS")
                .message("OTP sent successfully")
                .data(Map.of(
                        "channel", request.getChannel(),
                        "identifier", identifier
                ))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponseOutDto<String>> verify(@RequestBody OtpVerifyRequest request) {
        // Validate and sanitize request
        validationService.validateVerifyRequest(request);

        // Verify OTP
        otpService.verify(request.getIdentifier(), request.getOtp());

        ApiResponseOutDto<String> response = ApiResponseOutDto.<String>builder()
                .status("SUCCESS")
                .message("OTP verified successfully")
                .data(null)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponseOutDto<Map<String, Object>>> resend(@RequestBody OtpRequest request) {
        // Validate and sanitize request
        validationService.validateGenerateRequest(request);

        // Get identifier based on channel
        String identifier = validationService.getIdentifier(request);

        // Resend (generates new OTP after cooldown)
        String otp = otpService.resend(identifier);

        ApiResponseOutDto<Map<String, Object>> response = ApiResponseOutDto.<Map<String, Object>>builder()
                .status("SUCCESS")
                .message("New OTP sent successfully")
                .data(Map.of(
                        "channel", request.getChannel(),
                        "identifier", identifier,
                        "message", "Previous OTP has been invalidated"
                ))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }
}