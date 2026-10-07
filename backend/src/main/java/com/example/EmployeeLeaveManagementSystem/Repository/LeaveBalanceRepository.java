package com.example.EmployeeLeaveManagementSystem.Repository;

import com.example.EmployeeLeaveManagementSystem.Entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @EntityGraph(attributePaths = {"employee", "leaveType"})
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, int year);

    boolean existsByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, int year);

    /** Row lock used while approving / cancelling so balance updates are atomic. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select lb from LeaveBalance lb
            where lb.employee.id = :employeeId
              and lb.leaveType.id = :leaveTypeId
              and lb.year = :year
            """)
    Optional<LeaveBalance> findForUpdate(@Param("employeeId") Long employeeId,
                                         @Param("leaveTypeId") Long leaveTypeId,
                                         @Param("year") int year);
}
