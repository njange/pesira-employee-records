package io.pesira.records.employee;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.pesira.records.TestcontainersConfiguration;
import io.pesira.records.auth.TokenService;
import io.pesira.records.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// Each test adds three employees in its own department, so results don't depend on the seed data.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class EmployeeSearchApiTest {

    private static final String DEPARTMENT = "Test Lab";

    private final MockMvc mvc;
    private final TokenService tokens;
    private final AppUserRepository users;
    private final EmployeeRepository employees;

    EmployeeSearchApiTest(@Autowired MockMvc mvc, @Autowired TokenService tokens, @Autowired AppUserRepository users,
                          @Autowired EmployeeRepository employees) {
        this.mvc = mvc;
        this.tokens = tokens;
        this.users = users;
        this.employees = employees;
    }

    @BeforeEach
    void addEmployees() {
        add("Zara", "Quill", "QA_Lead", "50000", EmployeeStatus.ACTIVE);
        add("Yusuf", "Quill", "Engineer", "90000", EmployeeStatus.ON_LEAVE);
        add("Xena", "Brook", "Engineer", "70000", EmployeeStatus.ACTIVE);
    }

    @Test
    void listUsesDefaultPaging() throws Exception {
        mvc.perform(list())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(63));
    }

    @Test
    void searchIsCaseInsensitiveAndNarrowsResults() throws Exception {
        mvc.perform(list().param("search", "QUILL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].lastName", contains("Quill", "Quill")));
    }

    @Test
    void searchTreatsUnderscoreAndPercentLiterally() throws Exception {
        mvc.perform(list().param("department", DEPARTMENT).param("search", "_"))
                .andExpect(jsonPath("$.content[*].jobTitle", contains("QA_Lead")));
        mvc.perform(list().param("department", DEPARTMENT).param("search", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void departmentFilterIsCaseInsensitive() throws Exception {
        mvc.perform(list().param("department", "test LAB"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void statusFilterMatchesExactly() throws Exception {
        mvc.perform(list().param("department", DEPARTMENT).param("status", "ON_LEAVE"))
                .andExpect(jsonPath("$.content[*].firstName", contains("Yusuf")));
    }

    @Test
    void sortsBySalaryDescending() throws Exception {
        mvc.perform(list().param("department", DEPARTMENT).param("sort", "salary,desc"))
                .andExpect(jsonPath("$.content[*].firstName", contains("Yusuf", "Xena", "Zara")));
    }

    @Test
    void defaultSortIsLastNameThenId() throws Exception {
        mvc.perform(list().param("department", DEPARTMENT))
                .andExpect(jsonPath("$.content[*].firstName", contains("Xena", "Zara", "Yusuf")));
    }

    @Test
    void unknownSortFieldReturns400() throws Exception {
        mvc.perform(list().param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownSortDirectionReturns400() throws Exception {
        mvc.perform(list().param("sort", "salary,sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sizeAbove100Returns400() throws Exception {
        mvc.perform(list().param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void negativePageReturns400() throws Exception {
        mvc.perform(list().param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownStatusReturns400() throws Exception {
        mvc.perform(list().param("status", "RETIRED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagePastTheEndReturnsEmptyContentWithRealTotals() throws Exception {
        mvc.perform(list().param("department", DEPARTMENT).param("page", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    private MockHttpServletRequestBuilder list() {
        return get("/api/employees").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + tokens.issue(users.findByEmail("user@example.com").orElseThrow()).getTokenValue());
    }

    private void add(String firstName, String lastName, String jobTitle, String salary, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(firstName.toLowerCase() + "@test.example");
        employee.setDepartment(DEPARTMENT);
        employee.setJobTitle(jobTitle);
        employee.setSalary(new BigDecimal(salary));
        employee.setHireDate(LocalDate.of(2021, 3, 1));
        employee.setStatus(status);
        employees.save(employee);
    }
}
