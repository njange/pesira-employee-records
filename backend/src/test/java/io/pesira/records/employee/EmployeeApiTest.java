package io.pesira.records.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.pesira.records.TestcontainersConfiguration;
import io.pesira.records.auth.TokenService;
import io.pesira.records.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class EmployeeApiTest {

    private static final String ADMIN = "admin@example.com";
    private static final String USER = "user@example.com";

    private final MockMvc mvc;
    private final TokenService tokens;
    private final AppUserRepository users;

    EmployeeApiTest(@Autowired MockMvc mvc, @Autowired TokenService tokens, @Autowired AppUserRepository users) {
        this.mvc = mvc;
        this.tokens = tokens;
        this.users = users;
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/employees/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadEmployee() throws Exception {
        long id = create("ada@example.com");

        mvc.perform(get("/api/employees/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void userCannotCreate() throws Exception {
        mvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ada@example.com", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void userCannotDelete() throws Exception {
        long id = create("ada@example.com");

        mvc.perform(delete("/api/employees/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateReturns201WithLocationAndNormalizedValues() throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ada@Example.COM", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse();

        Number id = JsonPath.read(response.getContentAsString(), "$.id");
        assertThat(response.getHeader(HttpHeaders.LOCATION)).endsWith("/api/employees/" + id);
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":" ","lastName":"Lovelace","email":"not-an-email",
                                 "department":"Engineering","jobTitle":"Engineer","salary":-5,
                                 "hireDate":"2999-01-01","status":"ACTIVE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'firstName')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'salary')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'hireDate')]").exists());
    }

    @Test
    void duplicateEmailInAnyCaseReturns409() throws Exception {
        create("ada@example.com");

        mvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ADA@example.com", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateReturnsNewVersion() throws Exception {
        long id = create("ada@example.com");

        mvc.perform(put("/api/employees/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ada.lovelace@example.com", 0L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada.lovelace@example.com"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateToAnotherEmployeesEmailReturns409() throws Exception {
        create("ada@example.com");
        long id = create("grace@example.com");

        mvc.perform(put("/api/employees/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ada@example.com", 0L)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateWithStaleVersionReturns409() throws Exception {
        long id = create("ada@example.com");

        mvc.perform(put("/api/employees/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ada@example.com", 7L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The record was changed by someone else. Reload it and try again"));
    }

    @Test
    void updateWithoutVersionReturns400() throws Exception {
        long id = create("ada@example.com");

        mvc.perform(put("/api/employees/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ada@example.com", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mvc.perform(get("/api/employees/999999").header(HttpHeaders.AUTHORIZATION, bearer(USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getNonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/employees/abc").header(HttpHeaders.AUTHORIZATION, bearer(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204ThenGetReturns404() throws Exception {
        long id = create("ada@example.com");

        mvc.perform(delete("/api/employees/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isNotFound());
    }

    private long create(String email) throws Exception {
        String json = mvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static String body(String email, Long version) {
        return """
                {"firstName":"  Ada ","lastName":"Lovelace","email":"%s","department":"Engineering",
                 "jobTitle":"Software Engineer","salary":120000.50,"hireDate":"2020-01-15",
                 "status":"ACTIVE","version":%s}""".formatted(email, version);
    }

    private String bearer(String email) {
        return "Bearer " + tokens.issue(users.findByEmail(email).orElseThrow()).getTokenValue();
    }
}
