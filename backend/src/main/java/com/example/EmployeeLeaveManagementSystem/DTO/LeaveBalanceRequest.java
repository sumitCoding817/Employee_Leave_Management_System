package com.example.EmployeeLeaveManagementSystem.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LeaveBalanceRequest(
        @NotNull(message = "Employee is required")
        Long employeeId,

        @NotNull(message = "Leave type is required")
        Long leaveTypeId,

        @NotNull(message = "Total allocated days is required")
        @Min(value = 0, message = "Total allocated days cannot be negative")
        Integer totalAllocated,

        @Min(value = 0, message = "Used days cannot be negative")
        Integer usedDays,

        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be 2000 or later")
        @Max(value = 2100, message = "Year must be 2100 or earlier")
        Integer year) {
}
