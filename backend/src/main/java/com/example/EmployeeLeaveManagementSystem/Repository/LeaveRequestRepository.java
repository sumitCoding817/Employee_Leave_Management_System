package com.example.EmployeeLeaveManagementSystem.Repository;

import com.example.EmployeeLeaveManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    @EntityGraph(attributePaths = {"employee", "employee.department", "leaveType", "approvedBy"})
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "employee.department", "leaveType", "approvedBy"})
    Optional<LeaveRequest> findByIdAndEmployeeId(Long id, Long employeeId);

    @Override
    @EntityGraph(attributePaths = {"employee", "employee.department", "leaveType", "approvedBy"})
    Page<LeaveRequest> findAll(Specification<LeaveRequest> spec, Pageable pageable);

    /** Row lock so two HR users cannot approve / reject / cancel the same request concurrently. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lr from LeaveRequest lr where lr.id = :id")
    Optional<LeaveRequest> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(lr) from LeaveRequest lr
            where lr.employee.id = :employeeId
              and lr.status in :statuses
              and lr.startDate <= :endDate
              and lr.endDate >= :startDate
            """)
    long countOverlappingLeaves(@Param("employeeId") Long employeeId,
                                @Param("statuses") Collection<LeaveStatus> statuses,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    long countByStatus(LeaveStatus status);

    long countByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("""
            select lr.leaveType.name, lr.status, count(lr), coalesce(sum(lr.numberOfDays), 0)
            from LeaveRequest lr
            group by lr.leaveType.name, lr.status
            """)
    List<Object[]> leaveStatistics();
}
