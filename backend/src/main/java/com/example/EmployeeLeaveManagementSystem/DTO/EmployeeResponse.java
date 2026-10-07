package com.example.EmployeeLeaveManagementSystem.DTO;

import com.example.EmployeeLeaveManagementSystem.Enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Intentionally has NO password field. */
public record EmployeeResponse(Long id,
                               String employeeCode,
                               String firstName,
                               String lastName,
                               String email,
                               String phone,
                               Long departmentId,
                               String departmentName,
                               String designation,
                               LocalDate joiningDate,
                               Role role,
                               boolean active,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}
