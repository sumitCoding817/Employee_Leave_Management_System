package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveRequestDto;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RejectLeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "Leaves")
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Apply for leave (status is always PENDING)")
    public ApiResponse<LeaveResponse> apply(@Valid @RequestBody LeaveRequestDto request) {
        return ApiResponse.ok("Leave request submitted successfully", leaveService.applyLeave(request));
    }

    @GetMapping("/my")
    @Operation(summary = "My leave requests (paged)")
    public ApiResponse<PageResponse<LeaveResponse>> getMyLeaves(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok("Leave requests fetched successfully", leaveService.getMyLeaves(pageable));
    }

    @GetMapping("/my/{id}")
    @Operation(summary = "One of my leave requests")
    public ApiResponse<LeaveResponse> getMyLeave(@PathVariable Long id) {
        return ApiResponse.ok("Leave request fetched successfully", leaveService.getMyLeaveById(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel my PENDING (or future APPROVED) leave")
    public ApiResponse<LeaveResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Leave request cancelled successfully", leaveService.cancelLeave(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Approve a leave request - ADMIN/HR")
    public ApiResponse<LeaveResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok("Leave request approved successfully", leaveService.approveLeave(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Reject a leave request with a reason - ADMIN/HR")
    public ApiResponse<LeaveResponse> reject(@PathVariable Long id, @Valid @RequestBody RejectLeaveRequest request) {
        return ApiResponse.ok("Leave request rejected successfully", leaveService.rejectLeave(id, request));
    }
}
