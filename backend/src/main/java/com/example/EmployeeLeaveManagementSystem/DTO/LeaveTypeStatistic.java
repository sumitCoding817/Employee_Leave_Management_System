package com.example.EmployeeLeaveManagementSystem.DTO;

public record LeaveTypeStatistic(String leaveType,
                                 long pending,
                                 long approved,
                                 long rejected,
                                 long cancelled,
                                 long approvedDays) {
}
