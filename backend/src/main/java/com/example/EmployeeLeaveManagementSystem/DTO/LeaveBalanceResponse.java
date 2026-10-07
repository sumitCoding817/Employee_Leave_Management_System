package com.example.EmployeeLeaveManagementSystem.DTO;

public record LeaveBalanceResponse(Long id,
                                   Long employeeId,
                                   String employeeName,
                                   Long leaveTypeId,
                                   String leaveTypeName,
                                   int totalAllocated,
                                   int usedDays,
                                   int remainingDays,
                                   int year) {
}
