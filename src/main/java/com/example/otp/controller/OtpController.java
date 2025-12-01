package com.example.otp.controller;

import com.example.otp.model.OtpRequest;
import com.example.otp.model.OtpVerifyRequest;
import com.example.otp.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody OtpRequest request) {
        String otp = otpService.generate(request.getIdentifier());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully", "otp", otp));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody OtpVerifyRequest request) {
        otpService.verify(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody OtpRequest request) {
        String otp = otpService.resend(request.getIdentifier());
        return ResponseEntity.ok(Map.of("message", "OTP resent successfully", "otp", otp));
    }
}