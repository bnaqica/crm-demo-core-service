package com.bnaqica.crmdemocoreservice.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.dto.PromotedUser;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import com.bnaqica.crmdemocoreservice.service.PortalUserService;
import com.bnaqica.crmdemocoreservice.service.SubmissionService;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import com.bnaqica.crmdemocoreservice.support.SubmissionFixtures;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;

class SubmitterPromotionApiTests extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortalUserService portalUsers;

    @Autowired
    private SubmissionService submissions;

    private SubmissionFixtures fixtures;
    private final UUID teamA = UUID.randomUUID();
    private final UUID teamB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fixtures = new SubmissionFixtures(portalUsers, submissions);
    }

    private Submission inTeam(UUID team) {
        return fixtures.submission(team, "CLAIM", SubmissionStatus.RECEIVED, Instant.parse("2026-11-12T08:00:00Z"));
    }

    private ResultActions promoteAsMemberOfA(Submission submission) throws Exception {
        String sub = "member-" + UUID.randomUUID();
        when(ums.teamScope(sub)).thenReturn(new TeamScope(sub, "ACTIVE", Set.of(teamA), Set.of(), false));
        return mockMvc.perform(post("/submissions/" + submission.getId() + "/promote-submitter")
                .header("X-Correlation-Id", "corr-42")
                .with(jwt().jwt(token -> token.subject(sub))
                        .authorities(new SimpleGrantedAuthority("portal-users:write"))));
    }

    @Test
    void teamMemberPromotesTheStarterSubmitter() throws Exception {
        Submission submission = inTeam(teamA);
        UUID submitter = submission.getPortalUser().getId();
        when(umsPromotion.promote(submitter, "corr-42")).thenReturn(new PromotedUser("ACTIVE"));

        promoteAsMemberOfA(submission)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portalUserId").value(submitter.toString()))
                .andExpect(jsonPath("$.promoted").value(true))
                .andExpect(jsonPath("$.tier").value("STANDARD"));
        verify(umsPromotion).promote(submitter, "corr-42");
    }

    @Test
    void anotherTeamsSubmitterIsNotFoundAndTheUmsIsNotCalled() throws Exception {
        promoteAsMemberOfA(inTeam(teamB)).andExpect(status().isNotFound());

        verify(umsPromotion, never()).promote(any(), anyString());
    }

    @Test
    void umsConflictBecomesAConflictProblem() throws Exception {
        Submission submission = inTeam(teamA);
        when(umsPromotion.promote(eq(submission.getPortalUser().getId()), anyString()))
                .thenThrow(new UmsClientException(409, "not a starter"));

        promoteAsMemberOfA(submission)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void umsRateLimitBecomesTooManyRequests() throws Exception {
        Submission submission = inTeam(teamA);
        when(umsPromotion.promote(eq(submission.getPortalUser().getId()), anyString()))
                .thenThrow(new UmsClientException(429, "slow down"));

        promoteAsMemberOfA(submission).andExpect(status().isTooManyRequests());
    }

    @Test
    void otherUmsFailureIsABadGateway() throws Exception {
        Submission submission = inTeam(teamA);
        when(umsPromotion.promote(eq(submission.getPortalUser().getId()), anyString()))
                .thenThrow(new UmsClientException(500, "boom"));

        promoteAsMemberOfA(submission).andExpect(status().isBadGateway());
    }

    @Test
    void unreachableUmsIsABadGateway() throws Exception {
        Submission submission = inTeam(teamA);
        when(umsPromotion.promote(eq(submission.getPortalUser().getId()), anyString()))
                .thenThrow(new ResourceAccessException("connection refused"));

        promoteAsMemberOfA(submission).andExpect(status().isBadGateway());
    }
}
