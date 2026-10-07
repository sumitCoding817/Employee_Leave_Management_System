package com.example.EmployeeLeaveManagementSystem.Exception;

import org.springframework.http.HttpStatus;

/** Generic business-rule violation (HTTP 400). */
public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
