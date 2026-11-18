package com.bnaqica.crmdemocoreservice.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.support.TestJwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The local profile lets requests through the filter chain (Swagger UI works), but still reads a token sent with them;
 * CRM endpoints need one.
 */
@WebMvcTest(useDefaultFilters = false)
@ActiveProfiles("local")
@Import({SecurityConfig.class, TestJwts.DecoderConfig.class, LocalProfileSecurityTests.WidgetController.class})
class LocalProfileSecurityTests {

    private final MockMvc mockMvc;

    LocalProfileSecurityTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void localKeycloakTokenIsUsedForPermissions() throws Exception {
        mockMvc.perform(get("/test/widgets")
                        .header("Authorization", "Bearer " + TestJwts.crmUser("u1", "widgets:read")))
                .andExpect(status().isOk());
    }

    @Test
    void crmEndpointWithoutTokenAsksForOne() throws Exception {
        mockMvc.perform(get("/test/widgets")).andExpect(status().isUnauthorized());
    }

    @RestController
    static class WidgetController {

        @GetMapping("/test/widgets")
        @RequiresPermission(value = "widgets:read", description = "Read widgets")
        String list() {
            return "widgets";
        }
    }
}
