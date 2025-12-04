package com.example.otp.constants;


public class OtpConstants {


    public static final String REDIS_OTP_KEY_PREFIX = "OTP:";
    public static final String REDIS_RESEND_KEY_PREFIX = "RESEND:";
    public static final String REDIS_ATTEMPT_KEY_PREFIX = "ATTEMPTS:";


    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";


    public static final String ACTION_GENERATE = "GENERATE";
    public static final String ACTION_VERIFY = "VERIFY";
    public static final String ACTION_RESEND = "RESEND";


    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";


    public static final String FAILURE_OTP_EXPIRED = "OTP_EXPIRED";
    public static final String FAILURE_OTP_INVALID = "OTP_INVALID";
    public static final String FAILURE_OTP_NOT_FOUND = "OTP_NOT_FOUND";
    public static final String FAILURE_RESEND_COOLDOWN = "RESEND_COOLDOWN_ACTIVE";


    public static final String ERROR_INVALID_EMAIL = "Invalid email format";
    public static final String ERROR_INVALID_PHONE = "Invalid phone number format";
    public static final String ERROR_INVALID_OTP = "OTP must be exactly 6 digits";
    public static final String ERROR_INVALID_NAME = "Name contains invalid characters";
    public static final String ERROR_INVALID_CHANNEL = "Channel must be SMS or EMAIL";
    public static final String ERROR_MISSING_IDENTIFIER = "Identifier is required based on channel";

    private OtpConstants() {

    }
}