package com.example.EmployeeLeaveManagementSystem.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveTypeRequest(
        @NotBlank(message = "Leave type name is required")
        @Size(max = 50, message = "Leave type name must not exceed 50 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Total days is required")
        @Min(value = 0, message = "Total days cannot be negative")
        @Max(value = 366, message = "Total days cannot exceed 366")
        Integer totalDays) {
}
