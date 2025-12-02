package com.example.otp.controller;

import com.example.otp.model.dto.inDto.OtpRequest;
import com.example.otp.model.dto.inDto.OtpVerifyRequest;
import com.example.otp.model.dto.outDto.ApiResponseOutDto;
import com.example.otp.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponseOutDto<Map<String, String>>> generate(@RequestBody OtpRequest request) {
        String otp = otpService.generate(request.getIdentifier());

        ApiResponseOutDto<Map<String, String>> response = ApiResponseOutDto.<Map<String, String>>builder()
                .status("SUCCESS")
                .message("OTP sent successfully")
                .data(Map.of("otp", otp))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponseOutDto<String>> verify(@RequestBody OtpVerifyRequest request) {
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
    public ResponseEntity<ApiResponseOutDto<Map<String, String>>> resend(@RequestBody OtpRequest request) {
        String otp = otpService.resend(request.getIdentifier());

        ApiResponseOutDto<Map<String, String>> response = ApiResponseOutDto.<Map<String, String>>builder()
                .status("SUCCESS")
                .message("OTP resent successfully")
                .data(Map.of("otp", otp))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }
}