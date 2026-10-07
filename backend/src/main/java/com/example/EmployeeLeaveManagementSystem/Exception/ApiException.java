package com.example.EmployeeLeaveManagementSystem.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base class: every business exception carries the HTTP status it maps to. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
