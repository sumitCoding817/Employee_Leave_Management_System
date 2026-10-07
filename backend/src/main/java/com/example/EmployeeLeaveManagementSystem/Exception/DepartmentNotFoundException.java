package com.example.EmployeeLeaveManagementSystem.Exception;

public class DepartmentNotFoundException extends ResourceNotFoundException {

    public DepartmentNotFoundException(Long id) {
        super("Department not found with id: " + id);
    }
}
