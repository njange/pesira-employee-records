package io.pesira.records.employee;

import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class EmployeeSort {

    static final String DEFAULT = "lastName,asc";

    // API field -> entity property. Only these can be sorted on.
    private static final Map<String, String> FIELDS = Map.of(
            "firstName", "firstName",
            "lastName", "lastName",
            "email", "email",
            "department", "department",
            "jobTitle", "jobTitle",
            "salary", "salary",
            "hireDate", "hireDate",
            "status", "status");

    private EmployeeSort() {
    }

    // Parses "field,asc|desc". The id tie-breaker keeps paging stable when values repeat.
    static Sort parse(String value) {
        String[] parts = value.split(",", -1);
        String property = FIELDS.get(parts[0].trim());
        if (property == null || parts.length != 2) {
            throw invalid(value);
        }
        Sort.Direction direction = switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw invalid(value);
        };
        return Sort.by(direction, property).and(Sort.by("id"));
    }

    private static ResponseStatusException invalid(String value) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid sort '" + value + "'. Use field,asc or field,desc with one of " + FIELDS.keySet());
    }
}
