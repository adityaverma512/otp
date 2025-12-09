package com.example.otp.controller;

import com.example.otp.model.dto.outDto.ApiResponseOutDto;
import com.example.otp.model.dto.outDto.TestResultDto;
import com.example.otp.service.OtpTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/otp/test")
@Slf4j
public class OtpTestController {

    private final OtpTestService otpTestService;

    public OtpTestController(OtpTestService otpTestService) {
        this.otpTestService = otpTestService;
    }

    /**
     * Run all OTP service tests
     * This endpoint tests: Redis connection, OTP generation, verification, and resend
     */
    @PostMapping
    public ResponseEntity<ApiResponseOutDto<TestResultDto>> runTests() {
        log.info("🧪 Starting OTP service E2E tests");

        try {
            TestResultDto testResult = otpTestService.runAllTests();

            boolean allPassed = testResult.getTests().values().stream()
                    .allMatch(result -> "PASSED".equals(result));

            if (allPassed) {
                log.info("✅ All OTP service tests PASSED");

                ApiResponseOutDto<TestResultDto> response = ApiResponseOutDto.<TestResultDto>builder()
                        .status("SUCCESS")
                        .message("All OTP service tests passed successfully")
                        .data(testResult)
                        .timestamp(Instant.now())
                        .build();

                return ResponseEntity.ok(response);
            } else {
                log.error("❌ OTP service tests FAILED: {}", testResult.getFailedTest());

                ApiResponseOutDto<TestResultDto> response = ApiResponseOutDto.<TestResultDto>builder()
                        .status("ERROR")
                        .message("OTP service test failed: " + testResult.getFailedTest())
                        .data(testResult)
                        .timestamp(Instant.now())
                        .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            log.error("❌ OTP service test execution failed: {}", e.getMessage(), e);

            ApiResponseOutDto<TestResultDto> response = ApiResponseOutDto.<TestResultDto>builder()
                    .status("ERROR")
                    .message("OTP service test execution failed: " + e.getMessage())
                    .data(null)
                    .timestamp(Instant.now())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
