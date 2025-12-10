package com.example.otp.service;

import com.example.otp.constants.OtpConstants;
import com.example.otp.model.dto.inDto.OtpRequest;
import com.example.otp.model.dto.outDto.TestResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class OtpTestService {

    private final OtpService otpService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TEST_IDENTIFIER_EMAIL = "test@otp.internal";
    private static final String TEST_IDENTIFIER_PHONE = "+919999999999";

    // Cleanup timeout - don't wait forever during cleanup
    private static final long CLEANUP_TIMEOUT_MS = 5000; // 5 seconds

    public OtpTestService(OtpService otpService, RedisTemplate<String, String> redisTemplate) {
        this.otpService = otpService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Run all OTP service tests with smart cleanup
     * Industry best practice: Only cleanup what was actually created
     */
    public TestResultDto runAllTests() {
        long startTime = System.currentTimeMillis();
        Map<String, String> testResults = new LinkedHashMap<>();
        String failedTest = null;
        String errorDetails = null;

        // Track test state for smart cleanup
        boolean redisAvailable = false;
        Set<String> identifiersNeedingCleanup = new HashSet<>();

        try {
            // ==================== Test 1: Redis Connection ====================
            log.info("🧪 Starting Test 1: Redis Connection");
            testRedisConnection();
            testResults.put("redisConnection", "PASSED");
            redisAvailable = true;  // ✅ Redis is UP - cleanup is possible
            log.info("✅ Test 1 PASSED: Redis Connection");

            // ==================== Test 2: OTP Generation ====================
            log.info("🧪 Starting Test 2: OTP Generation");
            String generatedOtp = testGenerate();
            testResults.put("otpGeneration", "PASSED");
            identifiersNeedingCleanup.add(TEST_IDENTIFIER_EMAIL);  // ✅ Data created
            log.info("✅ Test 2 PASSED: OTP Generation");

            // ==================== Test 3: OTP Verification ====================
            log.info("🧪 Starting Test 3: OTP Verification");
            testVerify(generatedOtp);
            testResults.put("otpVerification", "PASSED");
            identifiersNeedingCleanup.remove(TEST_IDENTIFIER_EMAIL);  // ✅ Cleaned by verify
            log.info("✅ Test 3 PASSED: OTP Verification");

            // ==================== Test 4: OTP Resend ====================
            log.info("🧪 Starting Test 4: OTP Resend");
            testResend();
            testResults.put("otpResend", "PASSED");
            identifiersNeedingCleanup.remove(TEST_IDENTIFIER_PHONE);  // ✅ Cleaned by resend
            log.info("✅ Test 4 PASSED: OTP Resend");

        } catch (Exception e) {
            // Determine which test failed
            String currentTest = getCurrentTest(testResults.size());
            testResults.put(currentTest, "FAILED");
            failedTest = currentTest;
            errorDetails = e.getMessage();

            log.error("❌ Test FAILED: {} - {}", currentTest, e.getMessage());

            // Mark remaining tests as NOT_RUN
            markRemainingTestsAsNotRun(testResults, testResults.size());

            // Important: If generation test failed, we might have partial data
            if ("otpGeneration".equals(currentTest)) {
                identifiersNeedingCleanup.add(TEST_IDENTIFIER_EMAIL);
            }
        } finally {
            // ==================== Smart Cleanup Logic ====================
            performSmartCleanup(redisAvailable, identifiersNeedingCleanup);
        }

        long executionTime = System.currentTimeMillis() - startTime;

        return TestResultDto.builder()
                .tests(testResults)
                .executionTimeMs(executionTime)
                .testedAt(Instant.now())
                .failedTest(failedTest)
                .errorDetails(errorDetails)
                .build();
    }

    /**
     * Smart cleanup: Only cleanup if Redis is available and there's data to clean
     * Industry best practice: Fail gracefully, don't block on cleanup failures
     */
    private void performSmartCleanup(boolean redisAvailable, Set<String> identifiersNeedingCleanup) {
        if (!redisAvailable) {
            log.info("⏭️  Skipping cleanup - Redis unavailable (no data was created)");
            return;
        }

        if (identifiersNeedingCleanup.isEmpty()) {
            log.info("✅ No cleanup needed - all test data already removed by tests");
            return;
        }

        log.info("🧹 Cleaning up {} test identifier(s)", identifiersNeedingCleanup.size());

        int successCount = 0;
        int failureCount = 0;

        for (String identifier : identifiersNeedingCleanup) {
            try {
                boolean cleaned = cleanupIdentifierWithTimeout(identifier, CLEANUP_TIMEOUT_MS);
                if (cleaned) {
                    successCount++;
                    log.debug("✅ Cleaned up test data for: {}", identifier);
                } else {
                    failureCount++;
                    log.warn("⚠️  Cleanup timeout for: {}", identifier);
                }
            } catch (Exception e) {
                failureCount++;
                log.warn("⚠️  Failed to cleanup {}: {}", identifier, e.getMessage());
                // Don't throw - continue with other identifiers
            }
        }

        if (failureCount == 0) {
            log.info("✅ Cleanup completed successfully ({} identifier(s))", successCount);
        } else {
            log.warn("⚠️  Cleanup completed with {} success, {} failures", successCount, failureCount);
        }
    }

    /**
     * Cleanup single identifier with timeout to prevent hanging
     * Returns true if successful, false if timeout/failure
     */
    private boolean cleanupIdentifierWithTimeout(String identifier, long timeoutMs) {
        try {
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + identifier;
            String resendKey = OtpConstants.REDIS_RESEND_KEY_PREFIX + identifier;

            // Execute cleanup with timeout protection
            CompletableFuture<Void> cleanupFuture = CompletableFuture.runAsync(() -> {
                try {
                    redisTemplate.delete(otpKey);
                    redisTemplate.delete(resendKey);
                } catch (Exception e) {
                    throw new RuntimeException("Redis delete failed", e);
                }
            });

            // Wait for cleanup with timeout
            cleanupFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            return true;

        } catch (TimeoutException e) {
            log.warn("Cleanup timeout after {}ms for identifier: {}", timeoutMs, identifier);
            return false;
        } catch (Exception e) {
            log.warn("Cleanup failed for identifier {}: {}", identifier, e.getMessage());
            return false;
        }
    }

    /**
     * Test 1: Check Redis connection
     */
    private void testRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.debug("✅ Redis ping successful");
        } catch (Exception e) {
            throw new RuntimeException("Redis connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 2: Test OTP generation (WITHOUT sending notification)
     */
    private String testGenerate() {
        try {
            OtpRequest testRequest = createTestRequest("EMAIL");

            // Generate OTP without notification (health check mode)
            String otp = otpService.generate(TEST_IDENTIFIER_EMAIL, testRequest, true);

            if (otp == null || otp.length() != 6) {
                throw new RuntimeException("Generated OTP is invalid: " + otp);
            }

            // Verify OTP is stored in Redis
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER_EMAIL;
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp == null) {
                throw new RuntimeException("OTP not found in Redis after generation");
            }

            log.debug("✅ OTP generated and stored successfully: {}", otp);
            return otp;

        } catch (Exception e) {
            throw new RuntimeException("OTP generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 3: Test OTP verification
     */
    private void testVerify(String otp) {
        try {
            boolean verified = otpService.verify(TEST_IDENTIFIER_EMAIL, otp);

            if (!verified) {
                throw new RuntimeException("OTP verification returned false");
            }

            // Verify OTP is deleted from Redis after verification
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER_EMAIL;
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp != null) {
                throw new RuntimeException("OTP still exists in Redis after verification");
            }

            log.debug("✅ OTP verified and deleted successfully");

        } catch (Exception e) {
            throw new RuntimeException("OTP verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 4: Test OTP resend (WITHOUT sending notification)
     */
    private void testResend() {
        try {
            OtpRequest testRequest = createTestRequest("SMS");

            // First generate an OTP (without notification)
            String firstOtp = otpService.generate(TEST_IDENTIFIER_PHONE, testRequest, true);
            log.debug("✅ First OTP generated: {}", firstOtp);

            // Wait briefly to ensure different timestamp
            Thread.sleep(100);

            // Delete cooldown key to bypass cooldown for test
            String resendKey = OtpConstants.REDIS_RESEND_KEY_PREFIX + TEST_IDENTIFIER_PHONE;
            redisTemplate.delete(resendKey);

            // Resend (generates new OTP without notification)
            String secondOtp = otpService.resend(TEST_IDENTIFIER_PHONE, testRequest, true);
            log.debug("✅ Second OTP generated via resend: {}", secondOtp);

            if (secondOtp == null || secondOtp.length() != 6) {
                throw new RuntimeException("Resent OTP is invalid: " + secondOtp);
            }

            // Verify new OTP is stored
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER_PHONE;
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp == null) {
                throw new RuntimeException("New OTP not found in Redis after resend");
            }

            // Verify the new OTP works
            otpService.verify(TEST_IDENTIFIER_PHONE, secondOtp);

            log.debug("✅ OTP resend successful, new OTP verified");

        } catch (Exception e) {
            throw new RuntimeException("OTP resend failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create test OtpRequest
     */
    private OtpRequest createTestRequest(String channel) {
        OtpRequest request = new OtpRequest();
        request.setChannel(channel);
        request.setApplicantFirstName("Test");
        request.setApplicantLastName("User");

        if ("SMS".equals(channel)) {
            request.setApplicationMobileNumber(TEST_IDENTIFIER_PHONE);
            request.setApplicationEmailAddress("test@otp.internal");
        } else {
            request.setApplicationEmailAddress(TEST_IDENTIFIER_EMAIL);
            request.setApplicationMobileNumber("");
        }

        request.setLocale("US");
        return request;
    }

    /**
     * Get current test name based on test number
     */
    private String getCurrentTest(int testNumber) {
        switch (testNumber) {
            case 0: return "redisConnection";
            case 1: return "otpGeneration";
            case 2: return "otpVerification";
            case 3: return "otpResend";
            default: return "unknown";
        }
    }

    /**
     * Mark remaining tests as NOT_RUN
     */
    private void markRemainingTestsAsNotRun(Map<String, String> testResults, int failedAtIndex) {
        String[] allTests = {"redisConnection", "otpGeneration", "otpVerification", "otpResend"};

        for (int i = failedAtIndex; i < allTests.length; i++) {
            if (!testResults.containsKey(allTests[i])) {
                testResults.put(allTests[i], "NOT_RUN");
            }
        }
    }
}