package com.bnaqica.crmdemocoreservice.support;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Real PostgreSQL, Kafka and Keycloak (with the test realms from {@code src/test/resources/keycloak}). Import into
 * {@code @SpringBootTest} classes that need real tokens; Spring caches the context, so the containers start once per
 * test run.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestContainers {

    static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.8.1"; // same as local-docker
    static final String[] REALMS = {"crm-admin", "crm-internal", "portal"};

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2"));
    }

    @Bean
    KeycloakContainer keycloakContainer() {
        return new KeycloakContainer();
    }

    @Bean
    DynamicPropertyRegistrar keycloakProperties(KeycloakContainer keycloak) {
        return registry -> {
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloak.issuer("crm-internal"));
            registry.add(
                    "spring.security.oauth2.client.provider.crm-internal.token-uri",
                    () -> keycloak.tokenUri("crm-internal"));
        };
    }

    public static class KeycloakContainer extends GenericContainer<KeycloakContainer> {

        /** Given to {@code test-employee} in {@code crm-internal} by the test-only setup below. */
        /** The crm-internal test user; usernames there are email addresses. */
        public static final String TEST_EMPLOYEE = "test-employee@corp.example";

        public static final List<String> TEST_EMPLOYEE_ROLES = List.of("submissions:read", "ui:team-dashboard");

        private static final String KCADM = "/opt/keycloak/bin/kcadm.sh";

        KeycloakContainer() {
            super(DockerImageName.parse(KEYCLOAK_IMAGE));
            withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
            withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
            withEnv("KC_HEALTH_ENABLED", "true");
            for (String realm : REALMS) {
                withCopyFileToContainer(
                        MountableFile.forClasspathResource("keycloak/" + realm + "-realm.json"),
                        "/opt/keycloak/data/import/" + realm + "-realm.json");
            }
            withCommand("start-dev", "--import-realm");
            withExposedPorts(8080, 9000);
            waitingFor(Wait.forHttp("/health/ready")
                    .forPort(9000)
                    .withStartupTimeout(Duration.ofMinutes(6))); // slow when other containers run
        }

        /**
         * Test-only additions the generated realms don't have: {@code crm-api} client roles held by
         * {@code test-employee}, and a {@code crm-api} audience on {@code test-client} tokens (in real use the
         * {@code crm-app} client adds it). Also lets the master admin work over plain HTTP.
         */
        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            kcadm(
                    "config",
                    "credentials",
                    "--server",
                    "http://localhost:8080",
                    "--realm",
                    "master",
                    "--user",
                    "admin",
                    "--password",
                    "admin");
            kcadm("update", "realms/master", "-s", "sslRequired=NONE");
            String crmApi = clientUuid("crm-api");
            for (String role : TEST_EMPLOYEE_ROLES) {
                kcadm("create", "clients/" + crmApi + "/roles", "-r", "crm-internal", "-s", "name=" + role);
            }
            kcadm(
                    "create",
                    "clients/" + clientUuid("test-client") + "/protocol-mappers/models",
                    "-r",
                    "crm-internal",
                    "-s",
                    "name=audience-crm-api",
                    "-s",
                    "protocol=openid-connect",
                    "-s",
                    "protocolMapper=oidc-audience-mapper",
                    "-s",
                    "config.\"included.client.audience\"=crm-api",
                    "-s",
                    "config.\"access.token.claim\"=true");
            // --uusername can't find usernames containing '@', so look the id up
            List<String> addRoles = new ArrayList<>(List.of(
                    "add-roles", "-r", "crm-internal", "--uid", userId(TEST_EMPLOYEE), "--cclientid", "crm-api"));
            TEST_EMPLOYEE_ROLES.forEach(role -> addRoles.addAll(List.of("--rolename", role)));
            kcadm(addRoles.toArray(String[]::new));
        }

        private String clientUuid(String clientId) {
            return kcadm(
                            "get",
                            "clients",
                            "-r",
                            "crm-internal",
                            "-q",
                            "clientId=" + clientId,
                            "--fields",
                            "id",
                            "--format",
                            "csv",
                            "--noquotes")
                    .strip();
        }

        public String userId(String username) {
            return kcadm(
                            "get",
                            "users",
                            "-r",
                            "crm-internal",
                            "--fields",
                            "id,username",
                            "--format",
                            "csv",
                            "--noquotes")
                    .lines()
                    .filter(line -> line.endsWith("," + username))
                    .map(line -> line.substring(0, line.indexOf(',')))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No crm-internal user " + username));
        }

        private String kcadm(String... args) {
            String[] command = new String[args.length + 1];
            command[0] = KCADM;
            System.arraycopy(args, 0, command, 1, args.length);
            try {
                ExecResult result = execInContainer(command);
                if (result.getExitCode() != 0) {
                    throw new IllegalStateException(
                            "kcadm " + String.join(" ", args) + " failed: " + result.getStderr());
                }
                return result.getStdout();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        public String baseUrl() {
            return "http://" + getHost() + ":" + getMappedPort(8080);
        }

        public String issuer(String realm) {
            return baseUrl() + "/realms/" + realm;
        }

        public String tokenUri(String realm) {
            return issuer(realm) + "/protocol/openid-connect/token";
        }
    }
}
