package com.example.EmployeeLeaveManagementSystem.Repository;

import com.example.EmployeeLeaveManagementSystem.Entity.LeaveRequest;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Dynamic filters for the admin leave listing. A null filter is simply ignored. */
public final class LeaveRequestSpecifications {

    private LeaveRequestSpecifications() {
    }

    public static Specification<LeaveRequest> withFilters(LeaveStatus status,
                                                          Long employeeId,
                                                          String department,
                                                          LocalDate startDate,
                                                          LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (StringUtils.hasText(department)) {
                Path<String> departmentName = root.get("employee").get("department").get("name");
                predicates.add(cb.equal(cb.lower(departmentName), department.trim().toLowerCase()));
            }
            // Leaves that overlap the requested date window
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("startDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
