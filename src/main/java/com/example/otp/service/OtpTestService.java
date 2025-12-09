package com.example.otp.service;

import com.example.otp.constants.OtpConstants;
import com.example.otp.model.dto.outDto.TestResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class OtpTestService {

    private final OtpService otpService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TEST_IDENTIFIER = "test@otp.internal";

    public OtpTestService(OtpService otpService, RedisTemplate<String, String> redisTemplate) {
        this.otpService = otpService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Run all OTP service tests
     */
    public TestResultDto runAllTests() {
        long startTime = System.currentTimeMillis();
        Map<String, String> testResults = new LinkedHashMap<>();
        String failedTest = null;
        String errorDetails = null;

        try {
            // Test 1: Redis Connection
            log.info("🧪 Starting Test 1: Redis Connection");
            testRedisConnection();
            testResults.put("redisConnection", "PASSED");
            log.info("✅ Test 1 PASSED: Redis Connection");

            // Test 2: OTP Generation
            log.info("🧪 Starting Test 2: OTP Generation");
            String generatedOtp = testGenerate();
            testResults.put("otpGeneration", "PASSED");
            log.info("✅ Test 2 PASSED: OTP Generation");

            // Test 3: OTP Verification
            log.info("🧪 Starting Test 3: OTP Verification");
            testVerify(generatedOtp);
            testResults.put("otpVerification", "PASSED");
            log.info("✅ Test 3 PASSED: OTP Verification");

            // Test 4: OTP Resend
            log.info("🧪 Starting Test 4: OTP Resend");
            testResend();
            testResults.put("otpResend", "PASSED");
            log.info("✅ Test 4 PASSED: OTP Resend");

        } catch (Exception e) {
            // Mark current test as failed
            String currentTest = getCurrentTest(testResults.size());
            testResults.put(currentTest, "FAILED");
            failedTest = currentTest;
            errorDetails = e.getMessage();

            log.error("❌ Test FAILED: {} - {}", currentTest, e.getMessage());

            // Mark remaining tests as NOT_RUN
            markRemainingTestsAsNotRun(testResults, testResults.size());
        } finally {
            // Always cleanup
            cleanup();
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
     * Test 1: Check Redis connection
     */
    private void testRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.debug("Redis ping successful");
        } catch (Exception e) {
            throw new RuntimeException("Redis connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 2: Test OTP generation
     */
    private String testGenerate() {
        try {
            String otp = otpService.generate(TEST_IDENTIFIER);

            if (otp == null || otp.length() != 6) {
                throw new RuntimeException("Generated OTP is invalid: " + otp);
            }

            // Verify OTP is stored in Redis
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER;
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp == null) {
                throw new RuntimeException("OTP not found in Redis after generation");
            }

            log.debug("OTP generated and stored successfully: {}", otp);
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
            boolean verified = otpService.verify(TEST_IDENTIFIER, otp);

            if (!verified) {
                throw new RuntimeException("OTP verification returned false");
            }

            // Verify OTP is deleted from Redis after verification
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER;
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp != null) {
                throw new RuntimeException("OTP still exists in Redis after verification");
            }

            log.debug("OTP verified and deleted successfully");

        } catch (Exception e) {
            throw new RuntimeException("OTP verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test 4: Test OTP resend (with cooldown bypass)
     */
    private void testResend() {
        try {
            // First generate an OTP
            String firstOtp = otpService.generate(TEST_IDENTIFIER);
            log.debug("First OTP generated: {}", firstOtp);

            // Resend with cooldown bypass (for testing)
            String secondOtp = otpService.resend(TEST_IDENTIFIER, true);
            log.debug("Second OTP generated via resend: {}", secondOtp);

            if (secondOtp == null || secondOtp.length() != 6) {
                throw new RuntimeException("Resent OTP is invalid: " + secondOtp);
            }

            // Verify new OTP is stored
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER;
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp == null) {
                throw new RuntimeException("New OTP not found in Redis after resend");
            }

            // Verify the new OTP works
            otpService.verify(TEST_IDENTIFIER, secondOtp);

            log.debug("OTP resend successful, new OTP verified");

        } catch (Exception e) {
            throw new RuntimeException("OTP resend failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cleanup test data from Redis
     */
    private void cleanup() {
        try {
            String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + TEST_IDENTIFIER;
            String resendKey = OtpConstants.REDIS_RESEND_KEY_PREFIX + TEST_IDENTIFIER;

            redisTemplate.delete(otpKey);
            redisTemplate.delete(resendKey);

            log.debug("Test data cleaned up from Redis");
        } catch (Exception e) {
            log.error("Failed to cleanup test data: {}", e.getMessage());
        }
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