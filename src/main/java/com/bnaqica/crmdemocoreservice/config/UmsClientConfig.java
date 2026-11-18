package com.bnaqica.crmdemocoreservice.config;

import com.bnaqica.crmdemocoreservice.client.UmsClient;
import com.bnaqica.crmdemocoreservice.client.UmsPromotionClient;
import com.bnaqica.crmdemocoreservice.exception.UmsErrorTranslator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * UMS clients, each sending a client-credentials token of its own {@code crm-internal} client (registered under
 * {@code spring.security.oauth2.client}). {@link UmsClient} uses {@code core-service}; {@link UmsPromotionClient} uses
 * {@code crm-promoter}, the only client holding {@code ums:promote}.
 */
@Configuration
@EnableConfigurationProperties(CrmProperties.class)
public class UmsClientConfig {

    static final String CORE_SERVICE_REGISTRATION = "core-service";
    static final String CRM_PROMOTER_REGISTRATION = "crm-promoter";

    /** Works outside web requests too (Kafka listeners, startup). */
    @Bean
    OAuth2AuthorizedClientManager serviceAccountClientManager(
            ClientRegistrationRepository registrations, OAuth2AuthorizedClientService authorizedClients) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients);
    }

    @Bean
    UmsClient umsClient(CrmProperties properties, OAuth2AuthorizedClientManager clientManager) {
        return umsClient(
                RestClient.builder(),
                properties.ums().baseUrl(),
                clientManager,
                CORE_SERVICE_REGISTRATION,
                UmsClient.class);
    }

    @Bean
    UmsPromotionClient umsPromotionClient(CrmProperties properties, OAuth2AuthorizedClientManager clientManager) {
        return umsClient(
                RestClient.builder(),
                properties.ums().baseUrl(),
                clientManager,
                CRM_PROMOTER_REGISTRATION,
                UmsPromotionClient.class);
    }

    /** A UMS client of {@code type} authenticated as the {@code registrationId} client; public for tests. */
    public static <T> T umsClient(
            RestClient.Builder builder,
            String baseUrl,
            OAuth2AuthorizedClientManager clientManager,
            String registrationId,
            Class<T> type) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(clientManager);
        interceptor.setClientRegistrationIdResolver(request -> registrationId);
        // One cached token per client, whoever triggered the call
        interceptor.setPrincipalResolver(request -> new AnonymousAuthenticationToken(
                registrationId, registrationId, AuthorityUtils.createAuthorityList("ROLE_SERVICE")));
        RestClient restClient = builder.baseUrl(baseUrl)
                .requestInterceptor(interceptor)
                .defaultStatusHandler(HttpStatusCode::isError, UmsErrorTranslator::translate)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(type);
    }
}
