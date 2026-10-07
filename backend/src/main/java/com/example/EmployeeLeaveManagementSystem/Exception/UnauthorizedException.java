package com.example.EmployeeLeaveManagementSystem.Exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but is not allowed to perform this action (HTTP 403). */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
