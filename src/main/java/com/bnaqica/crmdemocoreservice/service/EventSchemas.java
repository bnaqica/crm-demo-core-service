package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.exception.InvalidEventException;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Validates events against the JSON Schemas in {@code src/main/resources/schemas}, by event type and
 * {@code schemaVersion}. Only the versions listed here are accepted; add a new version before producers emit it.
 */
@Component
public class EventSchemas {

    public static final String SUBMISSION_RECEIVED = "SubmissionReceived";
    public static final String PORTAL_USER_EMAIL_CHANGED = "PortalUserEmailChanged";
    /** {@code SubmissionAccepted} and {@code SubmissionRejected}, which share one schema. */
    public static final String SUBMISSION_STATUS = "SubmissionStatus";

    private static final Map<String, Map<Integer, String>> FILES = Map.of(
            SUBMISSION_RECEIVED, Map.of(1, "schemas/submission-received-v1.json"),
            PORTAL_USER_EMAIL_CHANGED, Map.of(1, "schemas/portal-user-email-changed-v1.json"),
            SUBMISSION_STATUS, Map.of(1, "schemas/submission-status-v1.json"));

    private final Map<String, Map<Integer, Schema>> schemas;

    public EventSchemas(JsonMapper jsonMapper) {
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        this.schemas = FILES.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        type -> type.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey, file -> load(registry, jsonMapper, file.getValue())))));
    }

    /** @throws InvalidEventException naming every violation */
    public void validate(String eventType, JsonNode event) {
        Map<Integer, Schema> versions = schemas.get(eventType);
        if (versions == null) {
            throw new InvalidEventException("Unknown event type " + eventType);
        }
        JsonNode version = event.get("schemaVersion");
        Schema schema = version != null && version.isInt() ? versions.get(version.asInt()) : null;
        if (schema == null) {
            throw new InvalidEventException("Unsupported schemaVersion " + version + " for " + eventType);
        }
        List<Error> errors = schema.validate(
                event, context -> context.executionConfig(config -> config.formatAssertionsEnabled(true)));
        if (!errors.isEmpty()) {
            throw new InvalidEventException("Invalid " + eventType + ": "
                    + errors.stream().map(Error::toString).collect(Collectors.joining("; ")));
        }
    }

    private static Schema load(SchemaRegistry registry, JsonMapper jsonMapper, String file) {
        try (InputStream in = new ClassPathResource(file).getInputStream()) {
            return registry.getSchema(jsonMapper.readTree(in));
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't read " + file, ex);
        }
    }
}
