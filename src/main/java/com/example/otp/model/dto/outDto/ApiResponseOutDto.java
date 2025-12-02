package com.example.otp.model.dto.outDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseOutDto<T> {
    private String status;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponseOutDto<T> success(final T data) {
        return ApiResponseOutDto.<T>builder()
                .status("success")
                .message("Data retrieved successfully.")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponseOutDto<T> success(final T data, final String msg) {
        return ApiResponseOutDto.<T>builder()
                .status("success")
                .message(msg)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponseOutDto<T> error(final String message) {
        return ApiResponseOutDto.<T>builder()
                .status("error")
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();
    }
}