package com.example.EmployeeLeaveManagementSystem.Exception;

public class LeaveTypeNotFoundException extends ResourceNotFoundException {

    public LeaveTypeNotFoundException(Long id) {
        super("Leave type not found with id: " + id);
    }
}
