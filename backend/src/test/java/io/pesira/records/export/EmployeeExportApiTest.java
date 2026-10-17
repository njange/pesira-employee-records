package io.pesira.records.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import io.pesira.records.TestcontainersConfiguration;
import io.pesira.records.auth.TokenService;
import io.pesira.records.employee.EmployeeRepository;
import io.pesira.records.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class EmployeeExportApiTest {

    private static final String CSV_HEADER =
            "id,firstName,lastName,email,department,jobTitle,salary,hireDate,status,createdAt,updatedAt";

    private final MockMvc mvc;
    private final TokenService tokens;
    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final JdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    EmployeeExportApiTest(@Autowired MockMvc mvc, @Autowired TokenService tokens, @Autowired AppUserRepository users,
                          @Autowired EmployeeRepository employees, @Autowired JdbcTemplate jdbc,
                          @Autowired JsonMapper jsonMapper) {
        this.mvc = mvc;
        this.tokens = tokens;
        this.users = users;
        this.employees = employees;
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    @Test
    void csvExportReturnsFileWithHeaderRow() throws Exception {
        MockHttpServletResponse response = mvc.perform(export().param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employees-" + LocalDate.now() + ".csv\""))
                .andReturn().getResponse();

        assertThat(MediaType.parseMediaType(response.getContentType()))
                .isEqualTo(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        List<String> lines = csvLines(response);
        assertThat(lines.getFirst()).isEqualTo(CSV_HEADER);
        assertThat(lines).hasSize((int) employees.count() + 1);
    }

    @Test
    void jsonExportParsesBackToEveryRow() throws Exception {
        MockHttpServletResponse response = mvc.perform(export().param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employees-" + LocalDate.now() + ".json\""))
                .andReturn().getResponse();

        List<?> rows = jsonMapper.readValue(response.getContentAsString(), List.class);
        assertThat(rows).hasSize((int) employees.count());
    }

    @Test
    void exportRespectsSearchAndSort() throws Exception {
        insertEmployees("zebedee", 2);

        MockHttpServletResponse response = mvc.perform(export()
                        .param("format", "csv")
                        .param("search", "ZEBEDEE")
                        .param("sort", "email,desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        List<String> lines = csvLines(response);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).contains("zebedee2@test.example");
        assertThat(lines.get(2)).contains("zebedee1@test.example");
    }

    @Test
    void exportStopsAt10000Rows() throws Exception {
        insertEmployees("bulk", 10_001);

        MockHttpServletResponse response = mvc.perform(export().param("format", "csv").param("search", "bulk"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(csvLines(response)).hasSize(10_000 + 1);
    }

    @Test
    void unknownFormatReturns400WithoutAttachment() throws Exception {
        mvc.perform(export().param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void missingFormatReturns400() throws Exception {
        mvc.perform(export())
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSortReturns400WithoutAttachment() throws Exception {
        mvc.perform(export().param("format", "csv").param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void exportWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/employees/export").param("format", "csv"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder export() {
        return get("/api/employees/export").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + tokens.issue(users.findByEmail("user@example.com").orElseThrow()).getTokenValue());
    }

    private static List<String> csvLines(MockHttpServletResponse response) throws Exception {
        return List.of(response.getContentAsString().split("\r\n"));
    }

    private void insertEmployees(String name, int count) {
        jdbc.update("""
                INSERT INTO employees (first_name, last_name, email, department, job_title, salary, hire_date, status)
                SELECT ?, 'Test', ? || n || '@test.example', 'Test Lab', 'Tester', 50000, DATE '2021-03-01', 'ACTIVE'
                FROM generate_series(1, ?) AS n""", name, name, count);
    }
}
