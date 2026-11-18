package com.bnaqica.crmdemocoreservice.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.support.TestJwts;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The production token validation: issuer ({@code crm-internal} only), audience ({@code crm-api}) and roles. */
@WebMvcTest(useDefaultFilters = false)
@Import({SecurityConfig.class, TestJwts.DecoderConfig.class, TokenTrustTests.AuthoritiesController.class})
class TokenTrustTests {

    private final MockMvc mockMvc;

    TokenTrustTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void portalTokenIsRejected() throws Exception {
        String token = TestJwts.token(TestJwts.PORTAL, "crm-api", "u1", List.of("submissions:read"));
        mockMvc.perform(get("/test/authorities").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminTokenIsRejected() throws Exception {
        String token = TestJwts.token(TestJwts.CRM_ADMIN, "crm-api", "u1", List.of("submissions:read"));
        mockMvc.perform(get("/test/authorities").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalTokenForAnotherAudienceIsRejected() throws Exception {
        String token = TestJwts.token(TestJwts.CRM_INTERNAL, "account", "u1", List.of("submissions:read"));
        mockMvc.perform(get("/test/authorities").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crmApiClientRolesBecomeAuthorities() throws Exception {
        String token = TestJwts.crmUser("u1", "submissions:read", "ui:team-dashboard");
        mockMvc.perform(get("/test/authorities").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("submissions:read,ui:team-dashboard")));
    }

    @RestController
    static class AuthoritiesController {

        @GetMapping("/test/authorities")
        String authorities(Authentication authentication) {
            return String.join(
                    ",",
                    authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .sorted()
                            .toList());
        }
    }
}
