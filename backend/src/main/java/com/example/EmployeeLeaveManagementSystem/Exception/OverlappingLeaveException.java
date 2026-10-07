package com.example.EmployeeLeaveManagementSystem.Exception;

import org.springframework.http.HttpStatus;

public class OverlappingLeaveException extends ApiException {

    public OverlappingLeaveException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
