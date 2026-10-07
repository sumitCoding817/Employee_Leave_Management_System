package com.example.EmployeeLeaveManagementSystem.Exception;

public class LeaveRequestNotFoundException extends ResourceNotFoundException {

    public LeaveRequestNotFoundException(Long id) {
        super("Leave request not found with id: " + id);
    }
}
