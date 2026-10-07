package com.example.EmployeeLeaveManagementSystem.Exception;

import org.springframework.http.HttpStatus;

public class InsufficientLeaveBalanceException extends ApiException {

    public InsufficientLeaveBalanceException(String leaveType, int remaining, int requested) {
        super(HttpStatus.BAD_REQUEST, "Leave balance is insufficient for " + leaveType
                + ": requested " + requested + " day(s), remaining " + remaining + " day(s)");
    }
}
