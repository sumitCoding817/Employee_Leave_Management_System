package com.example.EmployeeLeaveManagementSystem.DTO;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(long totalEmployees,
                                     long activeEmployees,
                                     long inactiveEmployees,
                                     long totalPendingLeaves,
                                     long totalApprovedLeaves,
                                     long totalRejectedLeaves,
                                     Map<String, Long> departmentWiseEmployeeCount,
                                     List<LeaveTypeStatistic> leaveStatistics) {
}
