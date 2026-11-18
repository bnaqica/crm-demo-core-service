package com.bnaqica.crmdemocoreservice.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Only tokens from the {@code crm-internal} realm ({@code JWT_ISSUER_URI}) issued for audience {@code crm-api} are
 * accepted; a {@code crm-admin} or {@code portal} token fails issuer validation and gets 401. The {@code crm-api}
 * client roles in the token ({@code resource_access.crm-api.roles}) become the caller's authorities, unprefixed
 * ({@code submissions:read}). Every endpoint except the health check needs a valid token; which permission it needs
 * is declared per endpoint with {@link RequiresPermission}. Browsers may call it from the configured CRM UI origins. CSRF protection is off because requests carry a bearer
 * token and there are no sessions or cookies.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(CrmProperties.class)
public class SecurityConfig {

    public static final String AUDIENCE = "crm-api";

    @Bean
    @Profile("!local")
    SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.requestMatchers("/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(clientRoleConverter())))
                .build();
    }

    /**
     * In the {@code local} profile no token is required to get past the filter chain (Swagger UI and the actuator work
     * without Keycloak), but a token sent along is still validated and used. CRM endpoints check permissions and the
     * UMS status on every call, so they need a token from the local Keycloak.
     */
    @Bean
    @Profile("local")
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2ResourceServer(
                        server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(clientRoleConverter())))
                .build();
    }

    /** Browsers may call the API only from {@code CRM_CORS_ALLOWED_ORIGINS} (the CRM UI). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CrmProperties properties) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.cors().allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /** Publishes authorization denials as {@code AuthorizationDeniedEvent}s, which are audited. */
    @Bean
    AuthorizationEventPublisher authorizationEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringAuthorizationEventPublisher(publisher);
    }

    /** Fills {@code {value}} in meta-annotations such as {@link RequiresPermission}. */
    @Bean
    static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(new CrmPermissionEvaluator());
        return handler;
    }

    /** Looks up the issuer's keys on first use, so the service starts while Keycloak is still coming up. */
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder =
                    NimbusJwtDecoder.withIssuerLocation(issuer).build();
            decoder.setJwtValidator(tokenValidator(issuer));
            return decoder;
        });
    }

    /** Expiry, the given issuer, and {@code crm-api} among the audiences. */
    public static OAuth2TokenValidator<Jwt> tokenValidator(String issuer) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD, audiences -> audiences != null && audiences.contains(AUDIENCE)));
    }

    static JwtAuthenticationConverter clientRoleConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CrmApiRoles());
        return converter;
    }

    private static final class CrmApiRoles implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess == null
                    || !(resourceAccess.get(AUDIENCE) instanceof Map<?, ?> client)
                    || !(client.get("roles") instanceof List<?> roles)) {
                return List.of();
            }
            return roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.toString()))
                    .toList();
        }
    }
}
