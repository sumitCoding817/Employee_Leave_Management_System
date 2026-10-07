package com.example.EmployeeLeaveManagementSystem.Service.Impl;

import com.example.EmployeeLeaveManagementSystem.Config.LeaveProperties;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveRequestDto;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RejectLeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveBalance;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import com.example.EmployeeLeaveManagementSystem.Exception.BusinessException;
import com.example.EmployeeLeaveManagementSystem.Exception.InsufficientLeaveBalanceException;
import com.example.EmployeeLeaveManagementSystem.Exception.InvalidLeaveDateException;
import com.example.EmployeeLeaveManagementSystem.Exception.LeaveAlreadyProcessedException;
import com.example.EmployeeLeaveManagementSystem.Exception.LeaveRequestNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.LeaveTypeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.OverlappingLeaveException;
import com.example.EmployeeLeaveManagementSystem.Exception.ResourceNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveBalanceRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveRequestRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveRequestSpecifications;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveBalanceService;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveService;
import com.example.EmployeeLeaveManagementSystem.Util.CurrentUserUtil;
import com.example.EmployeeLeaveManagementSystem.Util.EntityMapper;
import com.example.EmployeeLeaveManagementSystem.Util.LeaveDateUtil;
import com.example.EmployeeLeaveManagementSystem.Util.PageableUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveServiceImpl implements LeaveService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("appliedAt", "startDate", "endDate", "status", "numberOfDays");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "appliedAt");
    private static final List<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final CurrentUserUtil currentUserUtil;
    private final LeaveProperties leaveProperties;

    // ------------------------------------------------------------------ apply

    @Override
    @Transactional
    public LeaveResponse applyLeave(LeaveRequestDto request) {
        Employee employee = currentUserUtil.getCurrentEmployee();

        if (request.startDate().isAfter(request.endDate())) {
            throw new InvalidLeaveDateException("Start date cannot be after end date");
        }
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new InvalidLeaveDateException("Start date cannot be in the past");
        }
        if (request.startDate().getYear() != request.endDate().getYear()) {
            throw new InvalidLeaveDateException(
                    "A leave request cannot span two calendar years. Please submit separate requests");
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new LeaveTypeNotFoundException(request.leaveTypeId()));
        if (!leaveType.isActive()) {
            throw new BusinessException("Leave type is not active: " + leaveType.getName());
        }

        int days = LeaveDateUtil.calculateLeaveDays(
                request.startDate(), request.endDate(), leaveProperties.excludeWeekends());
        if (days <= 0) {
            throw new InvalidLeaveDateException("The selected dates contain no working days");
        }

        long overlapping = leaveRequestRepository.countOverlappingLeaves(
                employee.getId(), BLOCKING_STATUSES, request.startDate(), request.endDate());
        if (overlapping > 0) {
            throw new OverlappingLeaveException(
                    "You already have a PENDING or APPROVED leave overlapping these dates");
        }

        LeaveBalance balance = leaveBalanceService.getOrCreateBalance(
                employee, leaveType, request.startDate().getYear());
        if (balance.getRemainingDays() < days) {
            throw new InsufficientLeaveBalanceException(leaveType.getName(), balance.getRemainingDays(), days);
        }

        // NOTE: the balance is NOT touched here - it is deducted only when the leave is approved.
        LeaveRequest saved = leaveRequestRepository.save(LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .numberOfDays(days)
                .reason(request.reason().trim())
                .status(LeaveStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build());

        log.info("Leave applied: leaveId={}, employeeId={}, type={}, days={}",
                saved.getId(), employee.getId(), leaveType.getName(), days);
        return EntityMapper.toLeaveResponse(saved);
    }

    // ------------------------------------------------------------------ employee views

    @Override
    public PageResponse<LeaveResponse> getMyLeaves(Pageable pageable) {
        Employee employee = currentUserUtil.getCurrentEmployee();
        Page<LeaveRequest> page = leaveRequestRepository.findByEmployeeId(
                employee.getId(), PageableUtil.restrict(pageable, SORTABLE_FIELDS, DEFAULT_SORT));
        return PageResponse.from(page.map(EntityMapper::toLeaveResponse));
    }

    @Override
    public LeaveResponse getMyLeaveById(Long id) {
        Employee employee = currentUserUtil.getCurrentEmployee();
        return leaveRequestRepository.findByIdAndEmployeeId(id, employee.getId())
                .map(EntityMapper::toLeaveResponse)
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));
    }

    // ------------------------------------------------------------------ cancel

    @Override
    @Transactional
    public LeaveResponse cancelLeave(Long id) {
        Employee employee = currentUserUtil.getCurrentEmployee();
        LeaveRequest leave = leaveRequestRepository.findByIdForUpdate(id)
                .filter(l -> l.getEmployee().getId().equals(employee.getId()))
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));   // never reveal other people's requests

        switch (leave.getStatus()) {
            case PENDING -> {
                // nothing was deducted yet, so no balance change
            }
            case APPROVED -> {
                if (!leave.getStartDate().isAfter(LocalDate.now())) {
                    throw new LeaveAlreadyProcessedException(
                            "An approved leave that has already started cannot be cancelled");
                }
                LeaveBalance balance = lockBalance(leave);
                balance.setUsedDays(Math.max(0, balance.getUsedDays() - leave.getNumberOfDays()));
                balance.calculateRemainingDays();
                leaveBalanceRepository.save(balance);
            }
            default -> throw new LeaveAlreadyProcessedException("Leave request is already " + leave.getStatus());
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leave);
        log.info("Leave cancelled: leaveId={}, employeeId={}", id, employee.getId());
        return EntityMapper.toLeaveResponse(leave);
    }

    // ------------------------------------------------------------------ approve / reject

    @Override
    @Transactional
    public LeaveResponse approveLeave(Long id) {
        Employee approver = currentUserUtil.getCurrentEmployee();
        LeaveRequest leave = findPendingForUpdate(id);
        LeaveBalance balance = lockBalance(leave);

        if (balance.getRemainingDays() < leave.getNumberOfDays()) {
            throw new InsufficientLeaveBalanceException(
                    leave.getLeaveType().getName(), balance.getRemainingDays(), leave.getNumberOfDays());
        }

        // Balance update and status change happen in the same transaction (atomic)
        balance.setUsedDays(balance.getUsedDays() + leave.getNumberOfDays());
        balance.calculateRemainingDays();

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedAt(LocalDateTime.now());
        leave.setApprovedBy(approver);

        leaveBalanceRepository.save(balance);
        leaveRequestRepository.save(leave);
        log.info("Leave approved: leaveId={}, approvedBy={}, newUsedDays={}, newRemainingDays={}",
                id, approver.getId(), balance.getUsedDays(), balance.getRemainingDays());
        return EntityMapper.toLeaveResponse(leave);
    }

    @Override
    @Transactional
    public LeaveResponse rejectLeave(Long id, RejectLeaveRequest request) {
        Employee actor = currentUserUtil.getCurrentEmployee();
        LeaveRequest leave = findPendingForUpdate(id);

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectedAt(LocalDateTime.now());
        leave.setRejectionReason(request.rejectionReason().trim());

        leaveRequestRepository.save(leave);   // balance is untouched
        log.info("Leave rejected: leaveId={}, rejectedBy={}", id, actor.getId());
        return EntityMapper.toLeaveResponse(leave);
    }

    // ------------------------------------------------------------------ admin listing

    @Override
    public PageResponse<LeaveResponse> getAllLeaves(LeaveStatus status, Long employeeId, String department,
                                                    LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<LeaveRequest> page = leaveRequestRepository.findAll(
                LeaveRequestSpecifications.withFilters(status, employeeId, department, startDate, endDate),
                PageableUtil.restrict(pageable, SORTABLE_FIELDS, DEFAULT_SORT));
        return PageResponse.from(page.map(EntityMapper::toLeaveResponse));
    }

    // ------------------------------------------------------------------ helpers

    private LeaveRequest findPendingForUpdate(Long id) {
        LeaveRequest leave = leaveRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveAlreadyProcessedException("Leave request is already " + leave.getStatus());
        }
        return leave;
    }

    private LeaveBalance lockBalance(LeaveRequest leave) {
        return leaveBalanceRepository.findForUpdate(
                        leave.getEmployee().getId(), leave.getLeaveType().getId(), leave.getStartDate().getYear())
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for this leave request"));
    }
}
