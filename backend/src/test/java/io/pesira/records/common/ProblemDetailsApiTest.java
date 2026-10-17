package io.pesira.records.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.pesira.records.TestcontainersConfiguration;
import io.pesira.records.auth.TokenService;
import io.pesira.records.user.AppUserRepository;
import jakarta.validation.constraints.Max;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ProblemDetailsApiTest.ThrowingController.class})
class ProblemDetailsApiTest {

    private static final String SECRET = "internal detail that must not leak";

    private final MockMvc mvc;
    private final TokenService tokens;
    private final AppUserRepository users;

    ProblemDetailsApiTest(@Autowired MockMvc mvc, @Autowired TokenService tokens, @Autowired AppUserRepository users) {
        this.mvc = mvc;
        this.tokens = tokens;
        this.users = users;
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException(SECRET);
        }

        @GetMapping("/test/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        void adminOnly() {
        }

        @GetMapping("/test/authentication")
        void authentication() {
            throw new BadCredentialsException(SECRET);
        }

        @GetMapping("/test/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException(SECRET);
        }

        @GetMapping("/test/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/test/size")
        void size(@RequestParam @Max(100) int size) {
        }
    }

    @Test
    void missingTokenReturns401Problem() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value("/api/auth/me"));
    }

    @Test
    void invalidTokenReturns401Problem() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void malformedJsonReturns400Problem() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void invalidBodyReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value("2 fields are invalid"))
                .andExpect(jsonPath("$.instance").value("/api/auth/login"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
    }

    @Test
    void invalidRequestParamReturnsFieldErrors() throws Exception {
        mvc.perform(get("/test/size").param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer("user@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void wrongParamTypeReturns400() throws Exception {
        mvc.perform(get("/test/size").param("size", "abc").header(HttpHeaders.AUTHORIZATION, bearer("user@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternals() throws Exception {
        mvc.perform(get("/test/unexpected").header(HttpHeaders.AUTHORIZATION, bearer("admin@example.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString(SECRET))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void methodSecurityDenialReturns403NotA500() throws Exception {
        mvc.perform(get("/test/admin-only").header(HttpHeaders.AUTHORIZATION, bearer("user@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void authenticationExceptionReturns401NotA500() throws Exception {
        mvc.perform(get("/test/authentication").header(HttpHeaders.AUTHORIZATION, bearer("admin@example.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void dataIntegrityViolationReturns409() throws Exception {
        mvc.perform(get("/test/data-integrity").header(HttpHeaders.AUTHORIZATION, bearer("admin@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data"));
    }

    @Test
    void optimisticLockFailureReturns409() throws Exception {
        mvc.perform(get("/test/optimistic-lock").header(HttpHeaders.AUTHORIZATION, bearer("admin@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("changed by someone else")));
    }

    private String bearer(String email) {
        return "Bearer " + tokens.issue(users.findByEmail(email).orElseThrow()).getTokenValue();
    }
}
