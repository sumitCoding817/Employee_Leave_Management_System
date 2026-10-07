package com.example.EmployeeLeaveManagementSystem.Service.Impl;

import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeResponse;
import com.example.EmployeeLeaveManagementSystem.Entity.LeaveType;
import com.example.EmployeeLeaveManagementSystem.Exception.DuplicateResourceException;
import com.example.EmployeeLeaveManagementSystem.Exception.LeaveTypeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveTypeService;
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
public class LeaveTypeServiceImpl implements LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    @Override
    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        String name = request.name().trim();
        if (leaveTypeRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Leave type already exists: " + name);
        }
        LeaveType saved = leaveTypeRepository.save(LeaveType.builder()
                .name(name)
                .description(request.description())
                .totalDays(request.totalDays())
                .active(true)
                .build());
        log.info("Leave type created: id={}, name={}", saved.getId(), saved.getName());
        return EntityMapper.toLeaveTypeResponse(saved);
    }

    @Override
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll(Sort.by("name")).stream()
                .map(EntityMapper::toLeaveTypeResponse)
                .toList();
    }

    @Override
    public LeaveTypeResponse getLeaveTypeById(Long id) {
        return EntityMapper.toLeaveTypeResponse(findLeaveType(id));
    }

    @Override
    @Transactional
    public LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request) {
        LeaveType leaveType = findLeaveType(id);
        String name = request.name().trim();
        if (leaveTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Leave type already exists: " + name);
        }
        leaveType.setName(name);
        leaveType.setDescription(request.description());
        leaveType.setTotalDays(request.totalDays());
        return EntityMapper.toLeaveTypeResponse(leaveTypeRepository.save(leaveType));
    }

    /** Soft delete: existing requests and balances keep pointing at the leave type. */
    @Override
    @Transactional
    public void deleteLeaveType(Long id) {
        LeaveType leaveType = findLeaveType(id);
        leaveType.setActive(false);
        leaveTypeRepository.save(leaveType);
        log.info("Leave type deactivated: id={}", id);
    }

    private LeaveType findLeaveType(Long id) {
        return leaveTypeRepository.findById(id).orElseThrow(() -> new LeaveTypeNotFoundException(id));
    }
}
