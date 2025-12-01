package com.example.otp.service;

import com.example.otp.exception.InvalidOtpException;
import com.example.otp.exception.OtpNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.expirySeconds}")
    private int expirySeconds;

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(999999));
    }

    public String generate(String identifier) {
        String otp = generateOtp();
        String key = "OTP:" + identifier;

        redisTemplate.opsForValue().set(key, otp, expirySeconds, TimeUnit.SECONDS);

        log.info("Sending OTP to {} → OTP: {}", identifier, otp);
        System.out.println("Sending OTP to " + identifier + " → OTP: " + otp);

        return otp;
    }

    public boolean verify(String identifier, String otp) {
        String key = "OTP:" + identifier;
        String cachedOtp = redisTemplate.opsForValue().get(key);

        if (cachedOtp == null) {
            throw new OtpNotFoundException("OTP expired or not found for: " + identifier);
        }

        if (!cachedOtp.equals(otp)) {
            throw new InvalidOtpException("Invalid OTP for: " + identifier);
        }

        redisTemplate.delete(key);
        log.info("OTP verified and deleted for: {}", identifier);
        System.out.println("OTP verified and deleted for: " + identifier);

        return true;
    }

    public String resend(String identifier) {
        String key = "OTP:" + identifier;
        String existingOtp = redisTemplate.opsForValue().get(key);

        if (existingOtp != null) {
            redisTemplate.expire(key, expirySeconds, TimeUnit.SECONDS);
            log.info("Resending same OTP to {} → OTP: {}", identifier, existingOtp);
            System.out.println("Resending same OTP to " + identifier + " → OTP: " + existingOtp);
            return existingOtp;
        }

        return generate(identifier);
    }
}