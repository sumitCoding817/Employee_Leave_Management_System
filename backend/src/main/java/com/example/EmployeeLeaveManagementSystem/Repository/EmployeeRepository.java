package com.example.EmployeeLeaveManagementSystem.Repository;

import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @EntityGraph(attributePaths = "department")
    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

    boolean existsByDepartmentIdAndActiveTrue(Long departmentId);

    long countByActive(boolean active);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "department")
    @Query("""
            select e from Employee e
            where lower(e.firstName) like lower(concat('%', :keyword, '%'))
               or lower(e.lastName) like lower(concat('%', :keyword, '%'))
               or lower(e.email) like lower(concat('%', :keyword, '%'))
               or lower(e.employeeCode) like lower(concat('%', :keyword, '%'))
               or lower(concat(e.firstName, ' ', e.lastName)) like lower(concat('%', :keyword, '%'))
            """)
    Page<Employee> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("select d.name, count(e) from Employee e join e.department d group by d.name order by d.name")
    List<Object[]> countEmployeesByDepartment();
}
