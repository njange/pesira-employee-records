package io.pesira.records.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.pesira.records.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "app.cors.allowed-origins=https://app.example.com, https://app-*.example.com")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorsApiTest {

    private final MockMvc mvc;

    CorsApiTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    private ResultActions preflightLogin(String origin) throws Exception {
        return mvc.perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));
    }

    @Test
    void listedOriginIsAllowed() throws Exception {
        preflightLogin("https://app.example.com")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"));
    }

    @Test
    void originMatchingWildcardIsAllowed() throws Exception {
        preflightLogin("https://app-git-main.example.com")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://app-git-main.example.com"));
    }

    @Test
    void otherOriginIsRejected() throws Exception {
        preflightLogin("https://evil.example.com")
                .andExpect(status().isForbidden());
    }
}
