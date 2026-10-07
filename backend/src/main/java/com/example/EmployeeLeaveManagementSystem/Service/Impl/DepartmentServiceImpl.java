package com.example.EmployeeLeaveManagementSystem.Service.Impl;

import com.example.EmployeeLeaveManagementSystem.DTO.DepartmentRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.DepartmentResponse;
import com.example.EmployeeLeaveManagementSystem.Entity.Department;
import com.example.EmployeeLeaveManagementSystem.Exception.BusinessException;
import com.example.EmployeeLeaveManagementSystem.Exception.DepartmentNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Exception.DuplicateResourceException;
import com.example.EmployeeLeaveManagementSystem.Repository.DepartmentRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.DepartmentService;
import com.example.EmployeeLeaveManagementSystem.Util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Department already exists: " + name);
        }
        Department saved = departmentRepository.save(Department.builder()
                .name(name)
                .description(request.description())
                .active(true)
                .build());
        log.info("Department created: id={}, name={}", saved.getId(), saved.getName());
        return EntityMapper.toDepartmentResponse(saved);
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(EntityMapper::toDepartmentResponse)
                .toList();
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return EntityMapper.toDepartmentResponse(findDepartment(id));
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartment(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Department already exists: " + name);
        }
        department.setName(name);
        department.setDescription(request.description());
        return EntityMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    /** Soft delete. A department that still has active employees cannot be deactivated. */
    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = findDepartment(id);
        if (employeeRepository.existsByDepartmentIdAndActiveTrue(id)) {
            throw new BusinessException("Department has active employees and cannot be deactivated");
        }
        department.setActive(false);
        departmentRepository.save(department);
        log.info("Department deactivated: id={}", id);
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException(id));
    }
}
