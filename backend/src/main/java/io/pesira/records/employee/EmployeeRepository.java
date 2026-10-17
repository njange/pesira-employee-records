package io.pesira.records.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    // Emails are stored lowercase; callers must lowercase the input first.
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
