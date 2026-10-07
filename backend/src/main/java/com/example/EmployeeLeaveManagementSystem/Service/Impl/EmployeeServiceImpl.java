package com.example.EmployeeLeaveManagementSystem.Service.Impl;

import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RegisterRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.Department;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Enums.Role;
import com.example.EmployeeLeaveManagementSystem.Exception.BusinessException;
import com.example.EmployeeLeaveManagementSystem.Exception.DepartmentNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.EmailAlreadyExistsException;
import com.example.EmployeeLeaveManagementSystem.Exception.EmployeeCodeAlreadyExistsException;
import com.example.EmployeeLeaveManagementSystem.Exception.EmployeeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.UnauthorizedException;
import com.example.EmployeeLeaveManagementSystem.Repository.DepartmentRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.EmployeeService;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveBalanceService;
import com.example.EmployeeLeaveManagementSystem.Util.CurrentUserUtil;
import com.example.EmployeeLeaveManagementSystem.Util.EntityMapper;
import com.example.EmployeeLeaveManagementSystem.Util.PageableUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "employeeCode", "firstName", "lastName", "email", "designation", "joiningDate", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by("firstName");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveBalanceService leaveBalanceService;
    private final CurrentUserUtil currentUserUtil;

    @Override
    @Transactional
    public EmployeeResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String employeeCode = request.employeeCode().trim().toUpperCase();

        if (employeeRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new EmployeeCodeAlreadyExistsException(employeeCode);
        }
        Department department = findActiveDepartment(request.departmentId());

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .department(department)
                .designation(request.designation().trim())
                .joiningDate(request.joiningDate())
                .role(Role.EMPLOYEE)            // self-registration can never choose a role
                .active(true)
                .build();

        Employee saved = employeeRepository.save(employee);
        leaveBalanceService.initializeBalances(saved, Year.now().getValue());
        log.info("Employee registered: id={}, email={}", saved.getId(), saved.getEmail());
        return EntityMapper.toEmployeeResponse(saved);
    }

    @Override
    public PageResponse<EmployeeResponse> getAllEmployees(String keyword, Pageable pageable) {
        Pageable safePageable = PageableUtil.restrict(pageable, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<Employee> page = StringUtils.hasText(keyword)
                ? employeeRepository.search(keyword.trim(), safePageable)
                : employeeRepository.findAll(safePageable);
        return PageResponse.from(page.map(EntityMapper::toEmployeeResponse));
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee target = findEmployee(id);
        Employee actor = currentUserUtil.getCurrentEmployee();
        if (actor.getRole() == Role.EMPLOYEE && !actor.getId().equals(id)) {
            throw new UnauthorizedException("You can only view your own profile");
        }
        return EntityMapper.toEmployeeResponse(target);
    }

    @Override
    public EmployeeResponse getCurrentEmployee() {
        return EntityMapper.toEmployeeResponse(currentUserUtil.getCurrentEmployee());
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee target = findEmployee(id);
        Employee actor = currentUserUtil.getCurrentEmployee();

        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isHr = actor.getRole() == Role.HR;
        boolean isSelf = actor.getId().equals(target.getId());

        if (!isAdmin && !isHr && !isSelf) {
            throw new UnauthorizedException("You are not allowed to update this employee");
        }
        if (isHr && target.getRole() == Role.ADMIN) {
            throw new UnauthorizedException("HR cannot modify ADMIN accounts");
        }

        String newEmail = request.email() == null ? null : request.email().trim().toLowerCase();
        String newCode = request.employeeCode() == null ? null : request.employeeCode().trim().toUpperCase();

        if (isSelf && changed(request.role(), target.getRole())) {
            throw new UnauthorizedException("You cannot change your own role");
        }
        boolean adminOnlyChange = changed(newCode, target.getEmployeeCode())
                || changed(newEmail, target.getEmail())
                || changed(request.role(), target.getRole());
        if (adminOnlyChange && !isAdmin) {
            throw new UnauthorizedException("Only ADMIN can change employee code, email or role");
        }
        boolean managedChange = changed(request.departmentId(), target.getDepartment().getId())
                || changed(request.designation(), target.getDesignation())
                || changed(request.joiningDate(), target.getJoiningDate());
        if (managedChange && !isAdmin && !isHr) {
            throw new UnauthorizedException("Employees can only update their first name, last name and phone");
        }

        if (request.firstName() != null) {
            target.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            target.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            target.setPhone(request.phone().trim());
        }
        if (changed(newCode, target.getEmployeeCode())) {
            if (employeeRepository.existsByEmployeeCodeAndIdNot(newCode, id)) {
                throw new EmployeeCodeAlreadyExistsException(newCode);
            }
            target.setEmployeeCode(newCode);
        }
        if (changed(newEmail, target.getEmail())) {
            if (employeeRepository.existsByEmailAndIdNot(newEmail, id)) {
                throw new EmailAlreadyExistsException(newEmail);
            }
            target.setEmail(newEmail);
        }
        if (request.role() != null) {
            target.setRole(request.role());
        }
        if (changed(request.departmentId(), target.getDepartment().getId())) {
            target.setDepartment(findActiveDepartment(request.departmentId()));
        }
        if (request.designation() != null) {
            target.setDesignation(request.designation().trim());
        }
        if (request.joiningDate() != null) {
            target.setJoiningDate(request.joiningDate());
        }

        Employee saved = employeeRepository.save(target);
        log.info("Employee updated: id={} by userId={}", id, actor.getId());
        return EntityMapper.toEmployeeResponse(saved);
    }

    /** DELETE is a soft delete so leave history stays intact. ADMIN only. */
    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee target = findEmployee(id);
        Employee actor = currentUserUtil.getCurrentEmployee();
        if (actor.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only ADMIN can delete employees");
        }
        if (actor.getId().equals(id)) {
            throw new UnauthorizedException("You cannot delete your own account");
        }
        target.setActive(false);
        employeeRepository.save(target);
        log.info("Employee deactivated (deleted): id={} by userId={}", id, actor.getId());
    }

    @Override
    @Transactional
    public EmployeeResponse activateEmployee(Long id) {
        return changeActiveStatus(id, true);
    }

    @Override
    @Transactional
    public EmployeeResponse deactivateEmployee(Long id) {
        return changeActiveStatus(id, false);
    }

    private EmployeeResponse changeActiveStatus(Long id, boolean active) {
        Employee target = findEmployee(id);
        Employee actor = currentUserUtil.getCurrentEmployee();
        if (actor.getRole() == Role.EMPLOYEE) {
            throw new UnauthorizedException("Only ADMIN or HR can change employee status");
        }
        if (actor.getId().equals(id)) {
            throw new UnauthorizedException("You cannot change your own active status");
        }
        if (actor.getRole() == Role.HR && target.getRole() == Role.ADMIN) {
            throw new UnauthorizedException("HR cannot modify ADMIN accounts");
        }
        target.setActive(active);
        Employee saved = employeeRepository.save(target);
        log.info("Employee {}: id={} by userId={}", active ? "activated" : "deactivated", id, actor.getId());
        return EntityMapper.toEmployeeResponse(saved);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Department findActiveDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        if (!department.isActive()) {
            throw new BusinessException("Department is not active: " + department.getName());
        }
        return department;
    }

    private static <T> boolean changed(T newValue, T oldValue) {
        return newValue != null && !newValue.equals(oldValue);
    }
}
