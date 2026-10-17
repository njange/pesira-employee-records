package io.pesira.records.employee;

import java.util.Locale;

import io.pesira.records.common.ConflictException;
import io.pesira.records.common.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EmployeeService {

    private static final String DUPLICATE_EMAIL = "An employee with this email already exists";

    private final EmployeeRepository employees;

    public EmployeeService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(find(id));
    }

    public EmployeeResponse create(EmployeeRequest request) {
        if (employees.existsByEmail(normalizeEmail(request.email()))) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        Employee employee = new Employee();
        copy(request, employee);
        return EmployeeResponse.from(employees.saveAndFlush(employee));
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        if (request.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required to update an employee");
        }
        Employee employee = find(id);
        // Compared by hand: JPA does not reliably honour a version assigned to an already-loaded entity.
        // @Version still guards against two transactions racing on the same row.
        if (!request.version().equals(employee.getVersion())) {
            throw new ConflictException("The record was changed by someone else. Reload it and try again");
        }
        if (employees.existsByEmailAndIdNot(normalizeEmail(request.email()), id)) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        copy(request, employee);
        // Flush now so the response carries the new version and updatedAt.
        return EmployeeResponse.from(employees.saveAndFlush(employee));
    }

    public void delete(Long id) {
        employees.delete(find(id));
    }

    private Employee find(Long id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException("Employee " + id + " not found"));
    }

    private static void copy(EmployeeRequest request, Employee employee) {
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(normalizeEmail(request.email()));
        employee.setDepartment(request.department().trim());
        employee.setJobTitle(request.jobTitle().trim());
        employee.setSalary(request.salary());
        employee.setHireDate(request.hireDate());
        employee.setStatus(request.status());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
