package com.example.EmployeeLeaveManagementSystem.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(boolean success,
                               LocalDateTime timestamp,
                               int status,
                               String error,
                               String message,
                               String path,
                               Map<String, String> errors) {

    public static ApiErrorResponse of(HttpStatus status, String message, String path, Map<String, String> errors) {
        return new ApiErrorResponse(false, LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, path, errors);
    }
}
