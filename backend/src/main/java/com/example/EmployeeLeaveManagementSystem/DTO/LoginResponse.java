package com.example.EmployeeLeaveManagementSystem.DTO;

import com.example.EmployeeLeaveManagementSystem.Enums.Role;

public record LoginResponse(String token, String type, Long userId, String name, String email, Role role) {
}
