package com.bnaqica.crmdemocoreservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.service.PortalUserService;
import com.bnaqica.crmdemocoreservice.service.SubmissionService;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import com.bnaqica.crmdemocoreservice.support.SubmissionFixtures;
import com.bnaqica.crmdemocoreservice.support.TestKafka;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.kafka.KafkaContainer;

class SubmissionQueryApiTests extends IntegrationTest {

    private static final Instant MONDAY = Instant.parse("2026-11-09T10:00:00Z");
    private static final Instant WEDNESDAY = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortalUserService portalUsers;

    @Autowired
    private SubmissionService submissions;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private KafkaContainer kafkaContainer;

    private SubmissionFixtures fixtures;
    private final UUID teamA = UUID.randomUUID();
    private final UUID teamB = UUID.randomUUID();
    // Unique per test, so other tests' submissions in the shared database don't show up in filtered lists
    private final String type = "CLAIM-" + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fixtures = new SubmissionFixtures(portalUsers, submissions);
    }

    private ResultActions as(String sub, TeamScope scope, MockHttpServletRequestBuilder request) throws Exception {
        when(ums.teamScope(sub)).thenReturn(scope);
        return mockMvc.perform(request.with(
                jwt().jwt(token -> token.subject(sub)).authorities(new SimpleGrantedAuthority("submissions:read"))));
    }

    private ResultActions asMemberOfA(MockHttpServletRequestBuilder request) throws Exception {
        String sub = "member-" + UUID.randomUUID();
        return as(sub, new TeamScope(sub, "ACTIVE", Set.of(teamA), Set.of(), false), request);
    }

    @Test
    void memberListsOnlyTheirTeamsSubmissions() throws Exception {
        Submission inA = fixtures.submission(teamA, type, SubmissionStatus.RECEIVED, MONDAY);
        fixtures.submission(teamB, type, SubmissionStatus.RECEIVED, MONDAY);

        asMemberOfA(get("/submissions").param("type", type))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(inA.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listFiltersByStatusAndDate() throws Exception {
        fixtures.submission(teamA, type, SubmissionStatus.RECEIVED, MONDAY);
        Submission wanted = fixtures.submission(teamA, type, SubmissionStatus.ACCEPTED, WEDNESDAY);
        fixtures.submission(teamA, type, SubmissionStatus.ACCEPTED, MONDAY);

        asMemberOfA(get("/submissions")
                        .param("type", type)
                        .param("status", "ACCEPTED")
                        .param("receivedFrom", "2026-11-10T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(wanted.getId().toString()));
    }

    @Test
    void memberViewsTheirTeamsSubmissionWithDocuments() throws Exception {
        Submission inA = fixtures.submission(teamA, type, SubmissionStatus.RECEIVED, MONDAY);

        asMemberOfA(get("/submissions/" + inA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submitterEmail").value("submitter@acme.example"))
                .andExpect(jsonPath("$.documents[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.documents[0].size").value(1234))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void anotherTeamsSubmissionIsNotFound() throws Exception {
        Submission inB = fixtures.submission(teamB, type, SubmissionStatus.RECEIVED, MONDAY);

        asMemberOfA(get("/submissions/" + inB.getId())).andExpect(status().isNotFound());
    }

    @Test
    void superManagerReadsAnotherTeamAndTheReadIsAudited() throws Exception {
        Submission inB = fixtures.submission(teamB, type, SubmissionStatus.RECEIVED, MONDAY);
        String sub = "super-" + UUID.randomUUID();

        as(sub, new TeamScope(sub, "ACTIVE", Set.of(teamA), Set.of(), true), get("/submissions/" + inB.getId()))
                .andExpect(status().isOk());

        assertThat(TestKafka.await(
                        kafkaContainer,
                        "audit-events",
                        payload -> payload.contains("CROSS_TEAM_READ")
                                && payload.contains(inB.getId().toString()),
                        Duration.ofSeconds(30)))
                .hasSize(1);
    }

    @Test
    void submissionShowsTheSubmittersNewEmail() throws Exception {
        Submission inA = fixtures.submission(teamA, type, SubmissionStatus.RECEIVED, MONDAY);
        UUID submitter = inA.getPortalUser().getId();
        var record = new ProducerRecord<String, String>(
                "portal-intake", submitter.toString(), """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T09:00:00Z","portalUserId":"%s",
                 "email":"renamed@acme.example"}
                """.formatted(UUID.randomUUID(), submitter));
        record.headers().add("type", "PortalUserEmailChanged".getBytes(StandardCharsets.UTF_8));
        kafka.send(record);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> asMemberOfA(get("/submissions/" + inA.getId()))
                        .andExpect(jsonPath("$.submitterEmail").value("renamed@acme.example")));
    }
}
