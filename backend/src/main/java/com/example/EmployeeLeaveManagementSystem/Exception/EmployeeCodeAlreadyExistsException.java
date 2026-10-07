package com.example.EmployeeLeaveManagementSystem.Exception;

public class EmployeeCodeAlreadyExistsException extends DuplicateResourceException {

    public EmployeeCodeAlreadyExistsException(String employeeCode) {
        super("Employee code already exists: " + employeeCode);
    }
}
