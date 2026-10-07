package com.example.EmployeeLeaveManagementSystem.DTO;

public record LeaveTypeResponse(Long id, String name, String description, int totalDays, boolean active) {
}
