package com.example.EmployeeLeaveManagementSystem.DTO;

public record EmployeeDashboardResponse(long totalLeaveTypes,
                                        int totalAllocatedLeaves,
                                        int usedLeaves,
                                        int remainingLeaves,
                                        long pendingRequests,
                                        long approvedRequests,
                                        long rejectedRequests) {
}
