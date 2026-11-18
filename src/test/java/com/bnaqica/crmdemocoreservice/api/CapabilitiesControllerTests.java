package com.bnaqica.crmdemocoreservice.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.config.SecurityConfig;
import com.bnaqica.crmdemocoreservice.config.WebMvcConfig;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.GlobalExceptionHandler;
import com.bnaqica.crmdemocoreservice.processors.ActiveUserInterceptor;
import com.bnaqica.crmdemocoreservice.processors.PermissionInterceptor;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import com.bnaqica.crmdemocoreservice.service.CapabilitiesServiceImpl;
import com.bnaqica.crmdemocoreservice.service.TeamScopeService;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(CapabilitiesController.class)
@Import({
    SecurityConfig.class,
    WebMvcConfig.class,
    PermissionInterceptor.class,
    ActiveUserInterceptor.class,
    GlobalExceptionHandler.class,
    CapabilitiesServiceImpl.class
})
class CapabilitiesControllerTests {

    private static final UUID TEAM_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private final MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TeamScopeService teamScope;

    @MockitoBean
    private AuthorizationAuditService audit;

    CapabilitiesControllerTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void teamManagerSeesEffectivePermissionsFeaturesAndTeams() throws Exception {
        var scope = new TeamScope("u1", "ACTIVE", Set.of(TEAM_A), Set.of(TEAM_A), false);
        when(teamScope.cached("u1")).thenReturn(scope);
        when(teamScope.live("u1")).thenReturn(scope);

        mockMvc.perform(get("/me/capabilities").with(user("submissions:write", "ui:team-dashboard", "Team Manager")))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                {"sub":"u1","name":"Ana Test",
                                 "permissions":["submissions:read","submissions:write"],
                                 "uiFeatures":["team-dashboard"],
                                 "memberTeams":["%s"],"managedTeams":["%s"],"allTeams":false}
                                """.formatted(TEAM_A, TEAM_A), org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void activeUserWithoutPermissionsGets200() throws Exception {
        var scope = new TeamScope("u1", "ACTIVE", Set.of(), Set.of(), false);
        when(teamScope.cached("u1")).thenReturn(scope);
        when(teamScope.live("u1")).thenReturn(scope);

        mockMvc.perform(get("/me/capabilities").with(user()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"permissions\":[],\"uiFeatures\":[]}"));
    }

    @Test
    void disabledUserGets403() throws Exception {
        var scope = new TeamScope("u1", "DISABLED", Set.of(), Set.of(), false);
        when(teamScope.cached("u1")).thenReturn(scope);
        when(teamScope.live("u1")).thenReturn(scope);

        mockMvc.perform(get("/me/capabilities").with(user())).andExpect(status().isForbidden());
    }

    @Test
    void disabledUserStillCachedAsActiveGets403() throws Exception {
        when(teamScope.cached("u1")).thenReturn(new TeamScope("u1", "ACTIVE", Set.of(), Set.of(), false));
        when(teamScope.live("u1")).thenReturn(new TeamScope("u1", "DISABLED", Set.of(), Set.of(), false));

        mockMvc.perform(get("/me/capabilities").with(user())).andExpect(status().isForbidden());
    }

    private static RequestPostProcessor user(String... roles) {
        return jwt().jwt(token -> token.subject("u1").claim("name", "Ana Test"))
                .authorities(
                        Arrays.stream(roles).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
    }
}
