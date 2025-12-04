package com.example.otp.service;

import com.example.otp.config.ValidationConfig;
import com.example.otp.constants.OtpConstants;
import com.example.otp.exception.ValidationException;
import com.example.otp.model.dto.inDto.OtpRequest;
import com.example.otp.model.dto.inDto.OtpVerifyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@Slf4j
public class ValidationService {

    private final ValidationConfig validationConfig;

    public ValidationService(ValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
    }

    /**
     * Validate and sanitize Generate OTP request
     */
    public void validateGenerateRequest(OtpRequest request) {
        // Validate channel
        if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_CHANNEL);
        }

        String channel = request.getChannel().trim().toUpperCase();
        if (!channel.equals(OtpConstants.CHANNEL_SMS) && !channel.equals(OtpConstants.CHANNEL_EMAIL)) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_CHANNEL);
        }
        request.setChannel(channel);

        // Validate based on channel
        if (channel.equals(OtpConstants.CHANNEL_SMS)) {
            validateAndSanitizePhone(request);
        } else {
            validateAndSanitizeEmail(request);
        }

        // Validate names
        validateAndSanitizeName(request);
    }

    /**
     * Validate and sanitize Verify OTP request
     */
    public void validateVerifyRequest(OtpVerifyRequest request) {
        // Validate channel
        if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_CHANNEL);
        }

        String channel = request.getChannel().trim().toUpperCase();
        if (!channel.equals(OtpConstants.CHANNEL_SMS) && !channel.equals(OtpConstants.CHANNEL_EMAIL)) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_CHANNEL);
        }
        request.setChannel(channel);

        // Validate identifier
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new ValidationException(OtpConstants.ERROR_MISSING_IDENTIFIER);
        }

        // Sanitize identifier
        String identifier = sanitizeString(request.getIdentifier());
        if (channel.equals(OtpConstants.CHANNEL_EMAIL)) {
            identifier = identifier.toLowerCase();
        }
        request.setIdentifier(identifier);

        // Validate OTP format
        validateOtpFormat(request.getOtp());
    }

    private void validateAndSanitizePhone(OtpRequest request) {
        if (request.getApplicationMobileNumber() == null || request.getApplicationMobileNumber().trim().isEmpty()) {
            throw new ValidationException("Phone number is required for SMS channel");
        }

        String phone = sanitizeString(request.getApplicationMobileNumber());
        phone = phone.replaceAll("\\s+", ""); // Remove all spaces

        Pattern pattern = Pattern.compile(validationConfig.getPhone().getPattern());
        if (!pattern.matcher(phone).matches()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_PHONE);
        }

        if (phone.length() < validationConfig.getPhone().getMinLength() ||
                phone.length() > validationConfig.getPhone().getMaxLength()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_PHONE);
        }

        request.setApplicationMobileNumber(phone);
    }

    private void validateAndSanitizeEmail(OtpRequest request) {
        if (request.getApplicationEmailAddress() == null || request.getApplicationEmailAddress().trim().isEmpty()) {
            throw new ValidationException("Email address is required for EMAIL channel");
        }

        String email = sanitizeString(request.getApplicationEmailAddress());
        email = email.toLowerCase().trim();

        Pattern pattern = Pattern.compile(validationConfig.getEmail().getPattern());
        if (!pattern.matcher(email).matches()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_EMAIL);
        }

        if (email.length() > validationConfig.getEmail().getMaxLength()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_EMAIL);
        }

        request.setApplicationEmailAddress(email);
    }

    private void validateAndSanitizeName(OtpRequest request) {
        // Validate first name
        if (request.getApplicantFirstName() == null || request.getApplicantFirstName().trim().isEmpty()) {
            throw new ValidationException("First name is required");
        }

        String firstName = sanitizeString(request.getApplicantFirstName());
        Pattern pattern = Pattern.compile(validationConfig.getName().getPattern());
        if (!pattern.matcher(firstName).matches()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_NAME + " (First Name)");
        }
        request.setApplicantFirstName(firstName);

        // Validate last name
        if (request.getApplicantLastName() == null || request.getApplicantLastName().trim().isEmpty()) {
            throw new ValidationException("Last name is required");
        }

        String lastName = sanitizeString(request.getApplicantLastName());
        if (!pattern.matcher(lastName).matches()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_NAME + " (Last Name)");
        }
        request.setApplicantLastName(lastName);
    }

    private void validateOtpFormat(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new ValidationException("OTP is required");
        }

        String cleanOtp = otp.trim();
        Pattern pattern = Pattern.compile(validationConfig.getOtp().getPattern());
        if (!pattern.matcher(cleanOtp).matches()) {
            throw new ValidationException(OtpConstants.ERROR_INVALID_OTP);
        }
    }

    /**
     * Sanitize string - remove dangerous characters
     */
    private String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        // Trim whitespace
        String sanitized = input.trim();

        // Remove HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Remove SQL injection attempts
        sanitized = sanitized.replaceAll("[-'\"`;]", "");

        // Remove non-printable characters
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        return sanitized;
    }

    /**
     * Get identifier from request based on channel
     */
    public String getIdentifier(OtpRequest request) {
        if (OtpConstants.CHANNEL_SMS.equals(request.getChannel())) {
            return request.getApplicationMobileNumber();
        } else {
            return request.getApplicationEmailAddress();
        }
    }
}