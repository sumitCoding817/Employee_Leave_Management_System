package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.Config.LeaveProperties;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveRequestDto;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RejectLeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveBalance;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import com.example.EmployeeLeaveManagementSystem.Enums.Role;
import com.example.EmployeeLeaveManagementSystem.Exception.InsufficientLeaveBalanceException;
import com.example.EmployeeLeaveManagementSystem.Exception.InvalidLeaveDateException;
import com.example.EmployeeLeaveManagementSystem.Exception.LeaveAlreadyProcessedException;
import com.example.EmployeeLeaveManagementSystem.Exception.OverlappingLeaveException;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveBalanceRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveRequestRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.Impl.LeaveServiceImpl;
import com.example.EmployeeLeaveManagementSystem.Util.CurrentUserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private LeaveBalanceService leaveBalanceService;
    @Mock private CurrentUserUtil currentUserUtil;

    private final LocalDate start = LocalDate.now().plusDays(10);

    private LeaveServiceImpl leaveService;
    private Employee employee;
    private Employee hr;
    private LeaveType casual;
    private LeaveBalance balance;   // CASUAL: total 12, used 3, remaining 9

    @BeforeEach
    void setUp() {
        leaveService = new LeaveServiceImpl(leaveRequestRepository, leaveTypeRepository, leaveBalanceRepository,
                leaveBalanceService, currentUserUtil, new LeaveProperties(false));
        employee = Employee.builder().id(1L).firstName("John").lastName("Doe")
                .email("john@gmail.com").role(Role.EMPLOYEE).build();
        hr = Employee.builder().id(2L).firstName("Hannah").lastName("Roe")
                .email("hr@gmail.com").role(Role.HR).build();
        casual = LeaveType.builder().id(1L).name("CASUAL").totalDays(12).active(true).build();
        balance = LeaveBalance.builder().employee(employee).leaveType(casual)
                .totalAllocated(12).usedDays(3).year(start.getYear()).build();
        balance.calculateRemainingDays();
    }

    private LeaveRequest leave(int days, LeaveStatus status) {
        return LeaveRequest.builder().id(10L).employee(employee).leaveType(casual)
                .startDate(start).endDate(start.plusDays(days - 1)).numberOfDays(days)
                .reason("Family function").status(status).appliedAt(LocalDateTime.now()).build();
    }

    // ---------------------------------------------------------------- apply

    @Test
    void applyLeave_createsPendingRequest_andDoesNotDeductBalance() {
        LocalDate end = start.plusDays(4);
        when(currentUserUtil.getCurrentEmployee()).thenReturn(employee);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casual));
        when(leaveRequestRepository.countOverlappingLeaves(eq(1L), anyCollection(), eq(start), eq(end)))
                .thenReturn(0L);
        when(leaveBalanceService.getOrCreateBalance(employee, casual, start.getYear())).thenReturn(balance);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.applyLeave(new LeaveRequestDto(1L, start, end, "Family function"));

        assertEquals(LeaveStatus.PENDING, response.status());
        assertEquals(5, response.numberOfDays());
        assertNotNull(response.appliedAt());
        assertEquals(3, balance.getUsedDays());
        assertEquals(9, balance.getRemainingDays());
    }

    @Test
    void applyLeave_withInsufficientBalance_throws() {
        LocalDate end = start.plusDays(9);   // 10 days requested, only 9 remaining
        when(currentUserUtil.getCurrentEmployee()).thenReturn(employee);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casual));
        when(leaveRequestRepository.countOverlappingLeaves(eq(1L), anyCollection(), eq(start), eq(end)))
                .thenReturn(0L);
        when(leaveBalanceService.getOrCreateBalance(employee, casual, start.getYear())).thenReturn(balance);

        assertThrows(InsufficientLeaveBalanceException.class,
                () -> leaveService.applyLeave(new LeaveRequestDto(1L, start, end, "Long vacation")));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void applyLeave_withOverlappingLeave_throws() {
        LocalDate end = start.plusDays(2);
        when(currentUserUtil.getCurrentEmployee()).thenReturn(employee);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(casual));
        when(leaveRequestRepository.countOverlappingLeaves(eq(1L), anyCollection(), eq(start), eq(end)))
                .thenReturn(1L);

        assertThrows(OverlappingLeaveException.class,
                () -> leaveService.applyLeave(new LeaveRequestDto(1L, start, end, "Trip")));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void applyLeave_withInvalidDates_throws() {
        assertThrows(InvalidLeaveDateException.class, () -> leaveService.applyLeave(
                new LeaveRequestDto(1L, start.plusDays(5), start, "Start after end")));
        assertThrows(InvalidLeaveDateException.class, () -> leaveService.applyLeave(
                new LeaveRequestDto(1L, LocalDate.now().minusDays(1), start, "Start in the past")));
        verifyNoInteractions(leaveRequestRepository);
    }

    // ---------------------------------------------------------------- approve / reject

    @Test
    void approveLeave_deductsBalanceAtomically() {
        LeaveRequest pending = leave(5, LeaveStatus.PENDING);
        when(currentUserUtil.getCurrentEmployee()).thenReturn(hr);
        when(leaveRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pending));
        when(leaveBalanceRepository.findForUpdate(1L, 1L, start.getYear())).thenReturn(Optional.of(balance));

        LeaveResponse response = leaveService.approveLeave(10L);

        assertEquals(LeaveStatus.APPROVED, response.status());
        assertNotNull(pending.getApprovedAt());
        assertEquals(hr, pending.getApprovedBy());
        assertEquals(8, balance.getUsedDays());        // 3 + 5
        assertEquals(4, balance.getRemainingDays());   // 12 - 8
        verify(leaveBalanceRepository).save(balance);
    }

    @Test
    void approveLeave_whenAlreadyProcessed_throws() {
        when(currentUserUtil.getCurrentEmployee()).thenReturn(hr);
        when(leaveRequestRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(leave(5, LeaveStatus.APPROVED)));

        assertThrows(LeaveAlreadyProcessedException.class, () -> leaveService.approveLeave(10L));
        verifyNoInteractions(leaveBalanceRepository);
    }

    @Test
    void rejectLeave_storesReason_andLeavesBalanceUntouched() {
        LeaveRequest pending = leave(5, LeaveStatus.PENDING);
        when(currentUserUtil.getCurrentEmployee()).thenReturn(hr);
        when(leaveRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pending));

        LeaveResponse response = leaveService.rejectLeave(10L, new RejectLeaveRequest("Project deadline"));

        assertEquals(LeaveStatus.REJECTED, response.status());
        assertEquals("Project deadline", response.rejectionReason());
        assertNotNull(pending.getRejectedAt());
        verifyNoInteractions(leaveBalanceRepository);
        assertEquals(3, balance.getUsedDays());
        assertEquals(9, balance.getRemainingDays());
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void cancelLeave_pending_changesNoBalance() {
        LeaveRequest pending = leave(5, LeaveStatus.PENDING);
        when(currentUserUtil.getCurrentEmployee()).thenReturn(employee);
        when(leaveRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pending));

        LeaveResponse response = leaveService.cancelLeave(10L);

        assertEquals(LeaveStatus.CANCELLED, response.status());
        verifyNoInteractions(leaveBalanceRepository);
    }

    @Test
    void cancelLeave_approvedFutureLeave_restoresBalance() {
        balance.setUsedDays(8);
        balance.calculateRemainingDays();   // used 8, remaining 4
        LeaveRequest approved = leave(5, LeaveStatus.APPROVED);
        when(currentUserUtil.getCurrentEmployee()).thenReturn(employee);
        when(leaveRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(approved));
        when(leaveBalanceRepository.findForUpdate(1L, 1L, start.getYear())).thenReturn(Optional.of(balance));

        LeaveResponse response = leaveService.cancelLeave(10L);

        assertEquals(LeaveStatus.CANCELLED, response.status());
        assertEquals(3, balance.getUsedDays());
        assertEquals(9, balance.getRemainingDays());
    }
}
