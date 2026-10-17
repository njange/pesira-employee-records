package io.pesira.records.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 100) String jobTitle,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal salary,
        @NotNull @PastOrPresent LocalDate hireDate,
        @NotNull EmployeeStatus status,
        // Required on update (EmployeeService checks it); ignored on create.
        Long version) {
}
