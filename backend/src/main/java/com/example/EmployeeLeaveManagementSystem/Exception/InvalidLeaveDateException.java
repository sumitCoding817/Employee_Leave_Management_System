package com.example.EmployeeLeaveManagementSystem.Exception;

import org.springframework.http.HttpStatus;

public class InvalidLeaveDateException extends ApiException {

    public InvalidLeaveDateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
