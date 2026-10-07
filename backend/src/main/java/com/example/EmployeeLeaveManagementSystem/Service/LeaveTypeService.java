package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeResponse;

import java.util.List;

public interface LeaveTypeService {

    LeaveTypeResponse createLeaveType(LeaveTypeRequest request);

    List<LeaveTypeResponse> getAllLeaveTypes();

    LeaveTypeResponse getLeaveTypeById(Long id);

    LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request);

    void deleteLeaveType(Long id);
}
