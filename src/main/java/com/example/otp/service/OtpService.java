package com.example.otp.service;

import com.example.otp.config.OtpConfig;
import com.example.otp.constants.OtpConstants;
import com.example.otp.exception.InvalidOtpException;
import com.example.otp.exception.OtpNotFoundException;
import com.example.otp.exception.ResendCooldownException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final OtpConfig otpConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(RedisTemplate<String, String> redisTemplate, OtpConfig otpConfig) {
        this.redisTemplate = redisTemplate;
        this.otpConfig = otpConfig;
    }

    /**
     * Generate random OTP
     */
    private String generateOtp() {
        int max = (int) Math.pow(10, otpConfig.getLength()) - 1;
        int otp = secureRandom.nextInt(max);
        return String.format("%0" + otpConfig.getLength() + "d", otp);
    }

    /**
     * Hash OTP using configured algorithm
     */
    private String hashOtp(String otp) {
        if (otpConfig.getSecurity().getUseHashing()) {
            return DigestUtils.sha256Hex(otp);
        }
        return otp; // Fallback if hashing disabled
    }

    /**
     * Generate OTP for identifier
     */
    public String generate(String identifier) {
        String otp = generateOtp();
        String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + identifier;

        // Hash OTP before storing
        String hashedOtp = hashOtp(otp);

        // Store hashed OTP in Redis
        redisTemplate.opsForValue().set(
                otpKey,
                hashedOtp,
                otpConfig.getExpirySeconds(),
                TimeUnit.SECONDS
        );

        // Set resend cooldown timestamp
        String resendKey = OtpConstants.REDIS_RESEND_KEY_PREFIX + identifier;
        long resendAllowedAt = System.currentTimeMillis() +
                (otpConfig.getResend().getCooldownSeconds() * 1000L);
        redisTemplate.opsForValue().set(
                resendKey,
                String.valueOf(resendAllowedAt),
                otpConfig.getExpirySeconds(),
                TimeUnit.SECONDS
        );

        log.info("📧 OTP generated for identifier: {} (valid for {} seconds)",
                identifier, otpConfig.getExpirySeconds());
        System.out.println("📧 OTP generated for " + identifier + " → OTP: " + otp);

        return otp; // Return plain OTP (for sending to Salesforce)
    }

    /**
     * Verify OTP for identifier
     */
    public boolean verify(String identifier, String otp) {
        String otpKey = OtpConstants.REDIS_OTP_KEY_PREFIX + identifier;
        String storedHashedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedHashedOtp == null) {
            throw new OtpNotFoundException("OTP expired or not found for: " + identifier);
        }

        // Hash user's input OTP
        String hashedInputOtp = hashOtp(otp);

        // Compare hashes
        if (!storedHashedOtp.equals(hashedInputOtp)) {
            throw new InvalidOtpException("Invalid OTP for: " + identifier);
        }

        // Delete OTP after successful verification
        redisTemplate.delete(otpKey);

        // Also delete resend cooldown key
        String resendKey = OtpConstants.REDIS_RESEND_KEY_PREFIX + identifier;
        redisTemplate.delete(resendKey);

        log.info("✅ OTP verified and deleted for: {}", identifier);
        System.out.println("✅ OTP verified successfully for: " + identifier);

        return true;
    }

    /**
     * Resend OTP - Always generates NEW OTP after cooldown
     */
    public String resend(String identifier) {
        String resendKey = OtpConstants.REDIS_RESEND_KEY_PREFIX + identifier;
        String resendTimestamp = redisTemplate.opsForValue().get(resendKey);

        // Check if cooldown period is active
        if (resendTimestamp != null) {
            long resendAllowedAt = Long.parseLong(resendTimestamp);
            long currentTime = System.currentTimeMillis();

            if (currentTime < resendAllowedAt) {
                long remainingSeconds = (resendAllowedAt - currentTime) / 1000;
                throw new ResendCooldownException(
                        "Please wait before requesting a new OTP",
                        remainingSeconds
                );
            }
        }

        // Generate NEW OTP (invalidates old one)
        String newOtp = generate(identifier);

        log.info("🔄 New OTP generated for: {} (old OTP invalidated)", identifier);
        System.out.println("🔄 New OTP generated for " + identifier + " → OTP: " + newOtp);

        return newOtp;
    }
}