package com.example.EmployeeLeaveManagementSystem.Service.Impl;

import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceResponse;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveBalance;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;
import com.example.EmployeeLeaveManagementSystem.Exception.BusinessException;
import com.example.EmployeeLeaveManagementSystem.Exception.DuplicateResourceException;
import com.example.EmployeeLeaveManagementSystem.Exception.EmployeeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.LeaveTypeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.ResourceNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveBalanceRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveBalanceService;
import com.example.EmployeeLeaveManagementSystem.Util.CurrentUserUtil;
import com.example.EmployeeLeaveManagementSystem.Util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserUtil currentUserUtil;

    @Override
    @Transactional
    public List<LeaveBalanceResponse> getMyBalances(Integer year) {
        Employee employee = currentUserUtil.getCurrentEmployee();
        int resolvedYear = resolveYear(year);
        if (resolvedYear == Year.now().getValue()) {
            initializeBalances(employee, resolvedYear);   // lazily provisions a new year
        }
        return leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), resolvedYear).stream()
                .map(EntityMapper::toLeaveBalanceResponse)
                .toList();
    }

    @Override
    public List<LeaveBalanceResponse> getBalancesByEmployee(Long employeeId, Integer year) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, resolveYear(year)).stream()
                .map(EntityMapper::toLeaveBalanceResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveBalanceResponse createBalance(LeaveBalanceRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(request.employeeId()));
        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new LeaveTypeNotFoundException(request.leaveTypeId()));

        if (leaveBalanceRepository.existsByEmployeeIdAndLeaveTypeIdAndYear(
                employee.getId(), leaveType.getId(), request.year())) {
            throw new DuplicateResourceException("Leave balance already exists for this employee, leave type and year");
        }
        int used = request.usedDays() == null ? 0 : request.usedDays();
        validateUsage(request.totalAllocated(), used);

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .totalAllocated(request.totalAllocated())
                .usedDays(used)
                .year(request.year())
                .build();
        balance.calculateRemainingDays();
        LeaveBalance saved = leaveBalanceRepository.save(balance);
        log.info("Leave balance created: id={}, employeeId={}, leaveType={}, year={}",
                saved.getId(), employee.getId(), leaveType.getName(), saved.getYear());
        return EntityMapper.toLeaveBalanceResponse(saved);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse updateBalance(Long id, LeaveBalanceRequest request) {
        LeaveBalance balance = leaveBalanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found with id: " + id));

        if (!balance.getEmployee().getId().equals(request.employeeId())
                || !balance.getLeaveType().getId().equals(request.leaveTypeId())
                || balance.getYear() != request.year()) {
            throw new BusinessException("Employee, leave type and year of an existing balance cannot be changed");
        }
        int used = request.usedDays() == null ? balance.getUsedDays() : request.usedDays();
        validateUsage(request.totalAllocated(), used);

        balance.setTotalAllocated(request.totalAllocated());
        balance.setUsedDays(used);
        balance.calculateRemainingDays();
        log.info("Leave balance updated: id={}", id);
        return EntityMapper.toLeaveBalanceResponse(leaveBalanceRepository.save(balance));
    }

    @Override
    @Transactional
    public LeaveBalance getOrCreateBalance(Employee employee, LeaveType leaveType, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), leaveType.getId(), year)
                .orElseGet(() -> {
                    LeaveBalance created = LeaveBalance.builder()
                            .employee(employee)
                            .leaveType(leaveType)
                            .totalAllocated(leaveType.getTotalDays())
                            .usedDays(0)
                            .year(year)
                            .build();
                    created.calculateRemainingDays();
                    return leaveBalanceRepository.save(created);
                });
    }

    @Override
    @Transactional
    public void initializeBalances(Employee employee, int year) {
        for (LeaveType leaveType : leaveTypeRepository.findByActiveTrue()) {
            if (!leaveBalanceRepository.existsByEmployeeIdAndLeaveTypeIdAndYear(
                    employee.getId(), leaveType.getId(), year)) {
                getOrCreateBalance(employee, leaveType, year);
            }
        }
    }

    private int resolveYear(Integer year) {
        return year == null ? Year.now().getValue() : year;
    }

    private void validateUsage(int totalAllocated, int usedDays) {
        if (usedDays > totalAllocated) {
            throw new BusinessException("Used days cannot exceed total allocated days");
        }
    }
}
