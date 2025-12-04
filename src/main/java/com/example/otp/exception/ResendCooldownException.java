package com.example.otp.exception;

public class ResendCooldownException extends RuntimeException {

  private final long remainingSeconds;

  public ResendCooldownException(String message, long remainingSeconds) {
    super(message);
    this.remainingSeconds = remainingSeconds;
  }

  public long getRemainingSeconds() {
    return remainingSeconds;
  }
}