package com.example.otp.model.dto.outDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResultDto {

    /**
     * Map of test name to result (PASSED/FAILED/NOT_RUN)
     */
    private Map<String, String> tests;

    /**
     * Total execution time in milliseconds
     */
    private Long executionTimeMs;

    /**
     * When the test was executed
     */
    private Instant testedAt;

    /**
     * Name of the test that failed (if any)
     */
    private String failedTest;

    /**
     * Detailed error message (if any)
     */
    private String errorDetails;
}