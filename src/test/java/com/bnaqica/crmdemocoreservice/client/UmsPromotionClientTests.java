package com.bnaqica.crmdemocoreservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bnaqica.crmdemocoreservice.config.UmsClientConfig;
import com.bnaqica.crmdemocoreservice.dto.PromotedUser;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import java.time.Instant;
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

class UmsPromotionClientTests {

    private static final String UMS = "http://ums.test";

    private MockRestServiceServer server;
    private UmsPromotionClient client;
    private String requestedRegistration;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager clientManager = request -> {
            requestedRegistration = request.getClientRegistrationId();
            return new OAuth2AuthorizedClient(
                    ClientRegistration.withRegistrationId(request.getClientRegistrationId())
                            .clientId(request.getClientRegistrationId())
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .tokenUri("http://keycloak.test/token")
                            .build(),
                    "crm-promoter",
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "promoter-token",
                            Instant.now(),
                            Instant.now().plusSeconds(60)));
        };
        client = UmsClientConfig.umsClient(builder, UMS, clientManager, "crm-promoter", UmsPromotionClient.class);
    }

    @Test
    void promotesAsCrmPromoterWithTheCorrelationId() {
        UUID user = UUID.randomUUID();
        server.expect(requestTo(UMS + "/internal/users/" + user + "/promote"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer promoter-token"))
                .andExpect(header("X-Correlation-Id", "corr-7"))
                .andRespond(withSuccess(
                        "{\"sub\":\"%s\",\"userType\":\"EXTERNAL\",\"status\":\"ACTIVE\"}".formatted(user),
                        MediaType.APPLICATION_JSON));

        PromotedUser promoted = client.promote(user, "corr-7");

        assertThat(promoted.status()).isEqualTo("ACTIVE");
        assertThat(requestedRegistration).isEqualTo("crm-promoter");
        server.verify();
    }

    @Test
    void conflictKeepsItsStatus() {
        UUID user = UUID.randomUUID();
        server.expect(requestTo(UMS + "/internal/users/" + user + "/promote"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.promote(user, "corr-7"))
                .isInstanceOfSatisfying(
                        UmsClientException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(409));
    }
}
