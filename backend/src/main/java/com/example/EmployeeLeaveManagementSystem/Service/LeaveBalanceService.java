package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceResponse;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveBalance;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;

import java.util.List;

public interface LeaveBalanceService {

    List<LeaveBalanceResponse> getMyBalances(Integer year);

    List<LeaveBalanceResponse> getBalancesByEmployee(Long employeeId, Integer year);

    LeaveBalanceResponse createBalance(LeaveBalanceRequest request);

    LeaveBalanceResponse updateBalance(Long id, LeaveBalanceRequest request);

    /** Returns the balance for the year, creating it from the leave type's default allocation if missing. */
    LeaveBalance getOrCreateBalance(Employee employee, LeaveType leaveType, int year);

    /** Creates any missing balances (one per active leave type) for the employee and year. */
    void initializeBalances(Employee employee, int year);
}
