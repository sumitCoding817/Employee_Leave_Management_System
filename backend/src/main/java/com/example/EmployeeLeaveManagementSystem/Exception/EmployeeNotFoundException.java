package com.example.EmployeeLeaveManagementSystem.Exception;

public class EmployeeNotFoundException extends ResourceNotFoundException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found with id: " + id);
    }

    public EmployeeNotFoundException(String email) {
        super("Employee not found with email: " + email);
    }
}
