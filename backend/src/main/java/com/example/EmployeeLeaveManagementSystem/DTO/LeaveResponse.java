package com.example.EmployeeLeaveManagementSystem.DTO;

import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(Long id,
                            Long employeeId,
                            String employeeName,
                            String employeeCode,
                            String department,
                            Long leaveTypeId,
                            String leaveType,
                            LocalDate startDate,
                            LocalDate endDate,
                            int numberOfDays,
                            String reason,
                            LeaveStatus status,
                            LocalDateTime appliedAt,
                            LocalDateTime approvedAt,
                            LocalDateTime rejectedAt,
                            Long approvedById,
                            String approvedByName,
                            String rejectionReason) {
}
