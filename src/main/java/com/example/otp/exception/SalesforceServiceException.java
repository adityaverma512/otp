package com.example.otp.exception;

public class SalesforceServiceException extends RuntimeException {

    public SalesforceServiceException(String message) {
        super(message);
    }

    public SalesforceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}