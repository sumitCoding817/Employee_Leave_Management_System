package com.example.EmployeeLeaveManagementSystem.DTO;

import java.time.LocalDateTime;

public record DepartmentResponse(Long id, String name, String description, boolean active, LocalDateTime createdAt) {
}
