package com.example.EmployeeLeaveManagementSystem.Util;

import com.example.EmployeeLeaveManagementSystem.DTO.DepartmentResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeResponse;
import com.example.EmployeeLeaveManagementSystem.Entity.Department;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveBalance;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;

/** Entity -> DTO mapping. Must be called inside a transaction (lazy associations are read here). */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static EmployeeResponse toEmployeeResponse(Employee employee) {
        Department department = employee.getDepartment();
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                department != null ? department.getId() : null,
                department != null ? department.getName() : null,
                employee.getDesignation(),
                employee.getJoiningDate(),
                employee.getRole(),
                employee.isActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    public static DepartmentResponse toDepartmentResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getDescription(),
                department.isActive(), department.getCreatedAt());
    }

    public static LeaveTypeResponse toLeaveTypeResponse(LeaveType leaveType) {
        return new LeaveTypeResponse(leaveType.getId(), leaveType.getName(), leaveType.getDescription(),
                leaveType.getTotalDays(), leaveType.isActive());
    }

    public static LeaveBalanceResponse toLeaveBalanceResponse(LeaveBalance balance) {
        Employee employee = balance.getEmployee();
        return new LeaveBalanceResponse(
                balance.getId(),
                employee.getId(),
                fullName(employee),
                balance.getLeaveType().getId(),
                balance.getLeaveType().getName(),
                balance.getTotalAllocated(),
                balance.getUsedDays(),
                balance.getRemainingDays(),
                balance.getYear());
    }

    public static LeaveResponse toLeaveResponse(LeaveRequest leave) {
        Employee employee = leave.getEmployee();
        Employee approver = leave.getApprovedBy();
        Department department = employee.getDepartment();
        return new LeaveResponse(
                leave.getId(),
                employee.getId(),
                fullName(employee),
                employee.getEmployeeCode(),
                department != null ? department.getName() : null,
                leave.getLeaveType().getId(),
                leave.getLeaveType().getName(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getNumberOfDays(),
                leave.getReason(),
                leave.getStatus(),
                leave.getAppliedAt(),
                leave.getApprovedAt(),
                leave.getRejectedAt(),
                approver != null ? approver.getId() : null,
                approver != null ? fullName(approver) : null,
                leave.getRejectionReason());
    }

    private static String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
