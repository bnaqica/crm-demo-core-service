package com.bnaqica.crmdemocoreservice.support;

import com.bnaqica.crmdemocoreservice.config.SecurityConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Signs JWTs with a key generated for the test run. Import {@link TestJwts.DecoderConfig} to decode them with the
 * service's own issuer and audience checks, as if they came from Keycloak.
 */
public final class TestJwts {

    public static final String KEYCLOAK = "http://keycloak.test/realms/";
    public static final String CRM_INTERNAL = KEYCLOAK + "crm-internal";
    public static final String CRM_ADMIN = KEYCLOAK + "crm-admin";
    public static final String PORTAL = KEYCLOAK + "portal";

    private static final KeyPair KEYS = generate();

    private TestJwts() {}

    /** Replaces the Keycloak-backed decoder with one for the test key, keeping the production validators. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class DecoderConfig {

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEYS.getPublic())
                    .build();
            decoder.setJwtValidator(SecurityConfig.tokenValidator(CRM_INTERNAL));
            return decoder;
        }
    }

    /** A crm-internal token for {@code crm-api} holding the given crm-api client roles. */
    public static String crmUser(String sub, String... roles) {
        return token(CRM_INTERNAL, "crm-api", sub, List.of(roles));
    }

    public static String token(String issuer, String audience, String sub, List<String> crmApiRoles) {
        var claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(sub)
                .audience(audience)
                .claim("name", "Test " + sub)
                .claim("preferred_username", sub + "@corp.example")
                .claim("resource_access", Map.of("crm-api", Map.of("roles", crmApiRoles)))
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        try {
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEYS.getPrivate()));
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
        return jwt.serialize();
    }

    private static KeyPair generate() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
