package com.example.EmployeeLeaveManagementSystem.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_balance_employee_type_year",
                columnNames = {"employee_id", "leave_type_id", "leave_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Min(0)
    @Column(nullable = false)
    private int totalAllocated;

    @Min(0)
    @Column(nullable = false)
    private int usedDays;

    @Column(nullable = false)
    private int remainingDays;

    @Column(name = "leave_year", nullable = false)
    private int year;

    /** remainingDays = totalAllocated - usedDays (always kept in sync). */
    @PrePersist
    @PreUpdate
    public void calculateRemainingDays() {
        this.remainingDays = this.totalAllocated - this.usedDays;
    }
}
