package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.LeaveRequestDto;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RejectLeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LeaveService {

    LeaveResponse applyLeave(LeaveRequestDto request);

    PageResponse<LeaveResponse> getMyLeaves(Pageable pageable);

    LeaveResponse getMyLeaveById(Long id);

    LeaveResponse cancelLeave(Long id);

    LeaveResponse approveLeave(Long id);

    LeaveResponse rejectLeave(Long id, RejectLeaveRequest request);

    PageResponse<LeaveResponse> getAllLeaves(LeaveStatus status, Long employeeId, String department,
                                             LocalDate startDate, LocalDate endDate, Pageable pageable);
}
