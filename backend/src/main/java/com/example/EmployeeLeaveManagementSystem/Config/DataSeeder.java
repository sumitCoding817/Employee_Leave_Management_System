package com.example.EmployeeLeaveManagementSystem.Config;

import com.example.EmployeeLeaveManagementSystem.Entity.Department;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;
import com.example.EmployeeLeaveManagementSystem.Enums.Role;
import com.example.EmployeeLeaveManagementSystem.Repository.DepartmentRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

/** Idempotent seeding: every record is created only if it does not exist yet. */
@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(DepartmentRepository departmentRepository,
                      LeaveTypeRepository leaveTypeRepository,
                      EmployeeRepository employeeRepository,
                      LeaveBalanceService leaveBalanceService,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-email}") String adminEmail,
                      @Value("${app.seed.admin-password}") String adminPassword) {
        this.departmentRepository = departmentRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDepartments();
        seedLeaveTypes();
        seedAdmin();
    }

    private void seedDepartments() {
        Map<String, String> departments = new LinkedHashMap<>();
        departments.put("IT", "Information Technology");
        departments.put("HR", "Human Resources");
        departments.put("Finance", "Finance and Accounts");
        departments.put("Operations", "Operations and Administration");
        departments.put("Sales", "Sales and Marketing");

        departments.forEach((name, description) -> {
            if (!departmentRepository.existsByNameIgnoreCase(name)) {
                departmentRepository.save(Department.builder().name(name).description(description).active(true).build());
                log.info("Seeded department: {}", name);
            }
        });
    }

    private void seedLeaveTypes() {
        Map<String, Object[]> leaveTypes = new LinkedHashMap<>();
        leaveTypes.put("CASUAL", new Object[]{"Casual leave for personal matters", 12});
        leaveTypes.put("SICK", new Object[]{"Sick leave for illness or medical care", 10});
        leaveTypes.put("EARNED", new Object[]{"Earned / privilege leave", 15});
        leaveTypes.put("MATERNITY", new Object[]{"Maternity leave", 90});
        leaveTypes.put("PATERNITY", new Object[]{"Paternity leave", 15});
        leaveTypes.put("UNPAID", new Object[]{"Leave without pay", 30});

        leaveTypes.forEach((name, details) -> {
            if (!leaveTypeRepository.existsByNameIgnoreCase(name)) {
                leaveTypeRepository.save(LeaveType.builder()
                        .name(name)
                        .description((String) details[0])
                        .totalDays((Integer) details[1])
                        .active(true)
                        .build());
                log.info("Seeded leave type: {}", name);
            }
        });
    }

    private void seedAdmin() {
        if (!employeeRepository.existsByEmail(adminEmail)) {
            Department it = departmentRepository.findByNameIgnoreCase("IT")
                    .orElseThrow(() -> new IllegalStateException("Seed department IT is missing"));
            employeeRepository.save(Employee.builder()
                    .employeeCode("EMP0001")
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .phone("9999999999")
                    .department(it)
                    .designation("Administrator")
                    .joiningDate(LocalDate.now())
                    .role(Role.ADMIN)
                    .active(true)
                    .build());
            log.info("Seeded admin user: {}", adminEmail);
        }
        // Creates only the balances that are missing, so it is safe on every restart
        employeeRepository.findByEmail(adminEmail)
                .ifPresent(admin -> leaveBalanceService.initializeBalances(admin, Year.now().getValue()));
    }
}
