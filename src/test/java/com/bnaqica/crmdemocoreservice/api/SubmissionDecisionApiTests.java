package com.bnaqica.crmdemocoreservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SubmissionDecisionApiTests extends IntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortalUserService portalUsers;

    @Autowired
    private SubmissionService submissions;

    @Autowired
    private KafkaContainer kafka;

    private SubmissionFixtures fixtures;
    private final UUID teamA = UUID.randomUUID();
    private final UUID teamB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fixtures = new SubmissionFixtures(portalUsers, submissions);
    }

    private Submission received(UUID team) {
        return fixtures.submission(team, "CLAIM", SubmissionStatus.RECEIVED, Instant.parse("2026-11-12T08:00:00Z"));
    }

    private ResultActions as(TeamScope scope, MockHttpServletRequestBuilder request) throws Exception {
        when(ums.teamScope(scope.sub())).thenReturn(scope);
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(scope.sub()))
                .authorities(new SimpleGrantedAuthority("submissions:write"))));
    }

    private TeamScope memberOf(UUID team) {
        String sub = "member-" + UUID.randomUUID();
        return new TeamScope(sub, "ACTIVE", Set.of(team), Set.of(), false);
    }

    private static MockHttpServletRequestBuilder reject(Submission submission, String reason) {
        return post("/submissions/" + submission.getId() + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(java.util.Map.of("reason", reason, "version", 1)));
    }

    private static MockHttpServletRequestBuilder accept(Submission submission, int version) {
        return post("/submissions/" + submission.getId() + "/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + "}");
    }

    private JsonNode statusEvent(Submission submission) {
        var records = TestKafka.await(
                kafka,
                "submission-status",
                payload -> payload.contains(submission.getId().toString()),
                Duration.ofSeconds(30));
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().key())
                .isEqualTo(submission.getPortalUser().getId().toString());
        return JSON.readTree(records.getFirst().value());
    }

    @Test
    void teamMemberAcceptsAReceivedSubmission() throws Exception {
        Submission submission = received(teamA);

        as(memberOf(teamA), accept(submission, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(submissions.find(submission.getId()).orElseThrow().getStatus())
                .isEqualTo(SubmissionStatus.ACCEPTED);
        JsonNode event = statusEvent(submission);
        assertThat(event.get("status").asString()).isEqualTo("ACCEPTED");
        assertThat(event.has("reason")).isFalse();
    }

    @Test
    void rejectionPublishesTheReason() throws Exception {
        Submission submission = received(teamA);

        as(memberOf(teamA), reject(submission, "Missing invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Missing invoice"));

        JsonNode event = statusEvent(submission);
        assertThat(event.get("status").asString()).isEqualTo("REJECTED");
        assertThat(event.get("reason").asString()).isEqualTo("Missing invoice");
    }

    @Test
    void statusEventCarriesNoCrmData() throws Exception {
        Submission submission = received(teamA);

        as(memberOf(teamA), reject(submission, "Missing invoice")).andExpect(status().isOk());

        Set<String> fields = new HashSet<>();
        statusEvent(submission).propertyNames().forEach(fields::add);
        assertThat(fields)
                .containsExactlyInAnyOrder(
                        "eventId",
                        "schemaVersion",
                        "occurredAt",
                        "submissionId",
                        "version",
                        "portalUserId",
                        "status",
                        "reason");
    }

    @Test
    void decidingTwiceIsAConflict() throws Exception {
        Submission submission = received(teamA);
        TeamScope member = memberOf(teamA);
        as(member, reject(submission, "Missing invoice")).andExpect(status().isOk());

        as(member, accept(submission, 1)).andExpect(status().isConflict());
    }

    @Test
    void rejectionNeedsAReason() throws Exception {
        Submission submission = received(teamA);

        as(memberOf(teamA), reject(submission, " ")).andExpect(status().isBadRequest());
    }

    @Test
    void superManagerOutsideTheTeamCannotDecide() throws Exception {
        Submission submission = received(teamB);
        String sub = "super-" + UUID.randomUUID();

        as(new TeamScope(sub, "ACTIVE", Set.of(teamA), Set.of(), true), accept(submission, 1))
                .andExpect(status().isForbidden());
        assertThat(submissions.find(submission.getId()).orElseThrow().getStatus())
                .isEqualTo(SubmissionStatus.RECEIVED);
    }

    @Test
    void anotherTeamsSubmissionIsNotFound() throws Exception {
        Submission submission = received(teamB);

        as(memberOf(teamA), accept(submission, 1)).andExpect(status().isNotFound());
    }

    @Test
    void decidingAVersionThatWasSinceCorrectedIsAConflict() throws Exception {
        Submission submission = received(teamA);
        submission.setVersion(2); // a correction arrived after the user opened version 1
        submissions.save(submission);

        as(memberOf(teamA), accept(submission, 1)).andExpect(status().isConflict());
        assertThat(submissions.find(submission.getId()).orElseThrow().getStatus())
                .isEqualTo(SubmissionStatus.RECEIVED);
    }

    @Test
    void acceptNeedsTheDecidedVersion() throws Exception {
        Submission submission = received(teamA);

        as(
                        memberOf(teamA),
                        post("/submissions/" + submission.getId() + "/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staleCopyOfASubmissionCanNotBeSaved() throws Exception {
        Submission stale = received(teamA);
        as(memberOf(teamA), accept(stale, 1)).andExpect(status().isOk());

        stale.setStatus(SubmissionStatus.REJECTED); // decided from an older read, as a concurrent request would
        stale.setRejectionReason("Too late");
        assertThatThrownBy(() -> submissions.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
