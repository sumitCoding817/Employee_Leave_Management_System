package com.example.EmployeeLeaveManagementSystem.Exception;

import org.springframework.http.HttpStatus;

public class LeaveAlreadyProcessedException extends ApiException {

    public LeaveAlreadyProcessedException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
