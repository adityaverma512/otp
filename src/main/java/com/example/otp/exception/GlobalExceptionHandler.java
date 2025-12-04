package com.example.otp.exception;

import com.example.otp.model.dto.outDto.ApiResponseOutDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(OtpNotFoundException.class)
  public ResponseEntity<ApiResponseOutDto<String>> handleOtpNotFound(OtpNotFoundException ex) {
    ApiResponseOutDto<String> response = ApiResponseOutDto.<String>builder()
            .status("ERROR")
            .message(ex.getMessage())
            .data(null)
            .timestamp(Instant.now())
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(InvalidOtpException.class)
  public ResponseEntity<ApiResponseOutDto<String>> handleInvalidOtp(InvalidOtpException ex) {
    ApiResponseOutDto<String> response = ApiResponseOutDto.<String>builder()
            .status("ERROR")
            .message(ex.getMessage())
            .data(null)
            .timestamp(Instant.now())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(ResendCooldownException.class)
  public ResponseEntity<ApiResponseOutDto<Map<String, Object>>> handleResendCooldown(ResendCooldownException ex) {
    ApiResponseOutDto<Map<String, Object>> response = ApiResponseOutDto.<Map<String, Object>>builder()
            .status("ERROR")
            .message(ex.getMessage())
            .data(Map.of("remainingSeconds", ex.getRemainingSeconds()))
            .timestamp(Instant.now())
            .build();

    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiResponseOutDto<String>> handleValidation(ValidationException ex) {
    ApiResponseOutDto<String> response = ApiResponseOutDto.<String>builder()
            .status("ERROR")
            .message(ex.getMessage())
            .data(null)
            .timestamp(Instant.now())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponseOutDto<String>> handleGeneral(Exception ex) {
    ApiResponseOutDto<String> response = ApiResponseOutDto.<String>builder()
            .status("ERROR")
            .message("Internal server error: " + ex.getMessage())
            .data(null)
            .timestamp(Instant.now())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}