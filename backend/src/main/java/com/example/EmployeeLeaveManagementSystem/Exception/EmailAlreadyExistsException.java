package com.example.EmployeeLeaveManagementSystem.Exception;

public class EmailAlreadyExistsException extends DuplicateResourceException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
