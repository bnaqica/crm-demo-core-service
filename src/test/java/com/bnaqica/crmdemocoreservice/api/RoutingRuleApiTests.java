package com.bnaqica.crmdemocoreservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.service.SubmissionService;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RoutingRuleApiTests extends IntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private SubmissionService submissions;

    private final String region = "EU-" + UUID.randomUUID();

    @BeforeEach
    void activeUsers() {
        when(ums.teamScope(anyString()))
                .thenAnswer(
                        invocation -> new TeamScope(invocation.getArgument(0), "ACTIVE", Set.of(), Set.of(), false));
    }

    private ResultActions as(String permission, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject("admin-" + UUID.randomUUID()))
                .authorities(new SimpleGrantedAuthority(permission))));
    }

    private JsonNode create(String type, UUID team) throws Exception {
        String body = as(
                        "routing-rules:write",
                        post("/routing-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"type\":\"%s\",\"region\":\"%s\",\"teamId\":\"%s\"}"
                                        .formatted(type, region, team)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JSON.readTree(body);
    }

    @Test
    void ruleAddedThroughTheApiRoutesLaterSubmissions() throws Exception {
        UUID team = UUID.randomUUID();
        create("CLAIM", team);
        UUID submissionId = UUID.randomUUID();
        UUID portalUser = UUID.randomUUID();
        var record = new ProducerRecord<String, String>("portal-intake", portalUser.toString(), """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T09:00:00Z","portalUserId":"%s",
                 "email":"ana@acme.example","submissionId":"%s","version":1,"type":"CLAIM","region":"%s",
                 "documents":[]}
                """.formatted(
                        UUID.randomUUID(), portalUser, submissionId, region));
        record.headers().add("type", "SubmissionReceived".getBytes(StandardCharsets.UTF_8));
        kafka.send(record);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(submissions.find(submissionId))
                        .hasValueSatisfying(
                                submission -> assertThat(submission.getTeamId()).isEqualTo(team)));
    }

    @Test
    void newRuleGoesLastAndRulesCanBeReordered() throws Exception {
        JsonNode first = create("FIRST", UUID.randomUUID());
        JsonNode second = create("SECOND", UUID.randomUUID());
        assertThat(second.get("position").asInt())
                .isGreaterThan(first.get("position").asInt());

        List<String> order = new ArrayList<>();
        JSON.readTree(as("routing-rules:read", get("/routing-rules"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .forEach(rule -> order.add(rule.get("id").asString()));
        int a = order.indexOf(first.get("id").asString());
        int b = order.indexOf(second.get("id").asString());
        order.set(a, second.get("id").asString());
        order.set(b, first.get("id").asString());

        as(
                        "routing-rules:write",
                        put("/routing-rules/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON.writeValueAsString(java.util.Map.of("ruleIds", order))))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[%d].id".formatted(a)).value(second.get("id").asString()))
                .andExpect(
                        jsonPath("$[%d].id".formatted(b)).value(first.get("id").asString()));
    }

    @Test
    void reorderMustListEveryRuleOnce() throws Exception {
        JsonNode rule = create("TWICE", UUID.randomUUID());

        as(
                        "routing-rules:write",
                        put("/routing-rules/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"ruleIds\":[\"%1$s\",\"%1$s\"]}"
                                        .formatted(rule.get("id").asString())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void deletedRuleIsGone() throws Exception {
        JsonNode rule = create("GONE", UUID.randomUUID());
        String path = "/routing-rules/" + rule.get("id").asString();

        as("routing-rules:write", delete(path)).andExpect(status().isNoContent());
        as("routing-rules:write", delete(path)).andExpect(status().isNotFound());
    }

    @Test
    void ruleNeedsTypeRegionAndTeam() throws Exception {
        as(
                        "routing-rules:write",
                        post("/routing-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"type\":\"\",\"region\":\"EU\"}"))
                .andExpect(status().isBadRequest());
    }
}
