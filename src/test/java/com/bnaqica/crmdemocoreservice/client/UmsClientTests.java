package com.bnaqica.crmdemocoreservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bnaqica.crmdemocoreservice.config.UmsClientConfig;
import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class UmsClientTests {

    private static final String UMS = "http://ums.test";
    private static final UUID TEAM_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private MockRestServiceServer server;
    private UmsClient client;
    private String requestedRegistration;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager clientManager = request -> {
            requestedRegistration = request.getClientRegistrationId();
            return new OAuth2AuthorizedClient(
                    registration(request.getClientRegistrationId()),
                    request.getPrincipal().getName(),
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "service-token",
                            Instant.now(),
                            Instant.now().plusSeconds(60)));
        };
        client = UmsClientConfig.umsClient(builder, UMS, clientManager, "core-service", UmsClient.class);
    }

    @Test
    void readsTeamScopeWithServiceToken() {
        server.expect(requestTo(UMS + "/internal/users/u1/team-scope"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("""
                        {"sub":"u1","status":"ACTIVE","memberTeams":["%s"],"managedTeams":[],"allTeams":false}
                        """.formatted(TEAM_A), MediaType.APPLICATION_JSON));

        TeamScope scope = client.teamScope("u1");

        assertThat(scope.status()).isEqualTo("ACTIVE");
        assertThat(scope.memberTeams()).containsExactly(TEAM_A);
        assertThat(scope.managedTeams()).isEmpty();
        assertThat(scope.allTeams()).isFalse();
        assertThat(requestedRegistration).isEqualTo("core-service");
        server.verify();
    }

    @Test
    void registersCatalog() {
        server.expect(requestTo(UMS + "/internal/catalog/core-service"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(content().json("""
                                {"permissions":[{"key":"submissions:read","realm":"crm-internal",
                                  "description":"Read submissions","sensitive":false}]}
                                """))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.registerCatalog(
                "core-service",
                new CatalogRegistration(List.of(new CatalogRegistration.Permission(
                        "submissions:read", "crm-internal", "Read submissions", false))));

        server.verify();
    }

    @Test
    void unknownUserBecomesNotFoundException() {
        server.expect(requestTo(UMS + "/internal/users/u1/team-scope")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.teamScope("u1"))
                .isInstanceOfSatisfying(
                        UmsClientException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(404));
    }

    @Test
    void serverErrorBecomesClientException() {
        server.expect(requestTo(UMS + "/internal/users/u1/team-scope"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.teamScope("u1"))
                .isInstanceOfSatisfying(
                        UmsClientException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(503));
    }

    private static ClientRegistration registration(String id) {
        return ClientRegistration.withRegistrationId(id)
                .clientId(id)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://keycloak.test/token")
                .build();
    }
}
