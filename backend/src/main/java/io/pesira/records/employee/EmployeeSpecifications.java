package io.pesira.records.employee;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class EmployeeSpecifications {

    private static final char ESCAPE = '\\';

    private EmployeeSpecifications() {
    }

    // Combines only the filters the caller actually supplied.
    static Specification<Employee> filter(String search, String department, EmployeeStatus status) {
        Specification<Employee> spec = Specification.unrestricted();
        if (StringUtils.hasText(search)) {
            spec = spec.and(matchesSearch(search.trim()));
        }
        if (StringUtils.hasText(department)) {
            spec = spec.and(hasDepartment(department.trim()));
        }
        if (status != null) {
            spec = spec.and(hasStatus(status));
        }
        return spec;
    }

    static Specification<Employee> matchesSearch(String search) {
        String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("lastName")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("email")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("jobTitle")), pattern, ESCAPE));
    }

    static Specification<Employee> hasDepartment(String department) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("department")), department.toLowerCase(Locale.ROOT));
    }

    static Specification<Employee> hasStatus(EmployeeStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // Makes % and _ in the user's input match literally instead of acting as wildcards.
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
