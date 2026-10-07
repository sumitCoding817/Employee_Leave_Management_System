package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RegisterRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.Department;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Enums.Role;
import com.example.EmployeeLeaveManagementSystem.Exception.EmailAlreadyExistsException;
import com.example.EmployeeLeaveManagementSystem.Exception.EmployeeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.UnauthorizedException;
import com.example.EmployeeLeaveManagementSystem.Repository.DepartmentRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.Impl.EmployeeServiceImpl;
import com.example.EmployeeLeaveManagementSystem.Util.CurrentUserUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LeaveBalanceService leaveBalanceService;
    @Mock private CurrentUserUtil currentUserUtil;

    @InjectMocks private EmployeeServiceImpl employeeService;

    private RegisterRequest registerRequest() {
        return new RegisterRequest("emp100", "John", "Doe", "John@Gmail.com", "Password@123",
                "9876543210", 1L, "Developer", LocalDate.of(2024, 1, 15));
    }

    @Test
    void register_encodesPassword_forcesEmployeeRole_andInitializesBalances() {
        Department department = Department.builder().id(1L).name("IT").active(true).build();
        when(employeeRepository.existsByEmail("john@gmail.com")).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("EMP100")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("Password@123")).thenReturn("ENCODED");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        EmployeeResponse response = employeeService.register(registerRequest());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertEquals("ENCODED", captor.getValue().getPassword());
        assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
        assertEquals("john@gmail.com", response.email());
        assertEquals("EMP100", response.employeeCode());
        verify(leaveBalanceService).initializeBalances(any(Employee.class), anyInt());
    }

    @Test
    void register_withDuplicateEmail_throwsAndDoesNotSave() {
        when(employeeRepository.existsByEmail("john@gmail.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> employeeService.register(registerRequest()));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void getEmployeeById_whenMissing_throwsEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        EmployeeNotFoundException exception =
                assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(99L));
        assertEquals("Employee not found with id: 99", exception.getMessage());
    }

    @Test
    void updateEmployee_employeeCannotChangeOwnRole() {
        Department department = Department.builder().id(1L).name("IT").active(true).build();
        Employee employee = Employee.builder().id(1L).role(Role.EMPLOYEE).email("john@gmail.com")
                .employeeCode("EMP100").department(department).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(currentUserUtil.getCurrentEmployee()).thenReturn(employee);

        EmployeeRequest request = new EmployeeRequest(null, null, null, null, null, null, null, null, Role.ADMIN);

        assertThrows(UnauthorizedException.class, () -> employeeService.updateEmployee(1L, request));
        assertEquals(Role.EMPLOYEE, employee.getRole());
        verify(employeeRepository, never()).save(any(Employee.class));
    }
}
