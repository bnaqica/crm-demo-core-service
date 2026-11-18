package com.bnaqica.crmdemocoreservice.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import com.bnaqica.crmdemocoreservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocoreservice.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Token trust against tokens from a real Keycloak with the three realms. */
class RealTokenTrustTests extends IntegrationTest {

    private static final String SECURED = "/actuator/info";

    private final MockMvc mockMvc;
    private final TestTokens tokens;

    RealTokenTrustTests(@Autowired MockMvc mockMvc, @Autowired KeycloakContainer keycloak) {
        this.mockMvc = mockMvc;
        this.tokens = new TestTokens(keycloak);
    }

    @Test
    void portalTokenGets401() throws Exception {
        String token = tokens.userToken("portal", "test-portal@acme.example");
        mockMvc.perform(get(SECURED).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminTokenGets401() throws Exception {
        String token = tokens.userToken("crm-admin", "test-admin");
        mockMvc.perform(get(SECURED).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void internalTokenForCrmApiIsAccepted() throws Exception {
        String token = tokens.userToken("crm-internal", KeycloakContainer.TEST_EMPLOYEE);
        mockMvc.perform(get(SECURED).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
