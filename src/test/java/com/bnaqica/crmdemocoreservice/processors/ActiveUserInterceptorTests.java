package com.bnaqica.crmdemocoreservice.processors;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.config.SecurityConfig;
import com.bnaqica.crmdemocoreservice.config.WebMvcConfig;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.GlobalExceptionHandler;
import com.bnaqica.crmdemocoreservice.exception.UmsUnavailableException;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import com.bnaqica.crmdemocoreservice.service.TeamScopeService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(useDefaultFilters = false)
@Import({
    SecurityConfig.class,
    WebMvcConfig.class,
    PermissionInterceptor.class,
    ActiveUserInterceptor.class,
    GlobalExceptionHandler.class,
    ActiveUserInterceptorTests.NoteController.class
})
class ActiveUserInterceptorTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TeamScopeService teamScope;

    @MockitoBean
    private AuthorizationAuditService audit;

    ActiveUserInterceptorTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void userDisabledAfterTokenWasIssuedGets403OnWrite() throws Exception {
        when(teamScope.live("u1")).thenReturn(scope("DISABLED"));

        mockMvc.perform(post("/test/notes").with(user("notes:write"))).andExpect(status().isForbidden());
        verify(audit).denied("notes:write", "user status DISABLED");
    }

    @Test
    void writeChecksLiveStatusEvenWhenCacheSaysActive() throws Exception {
        when(teamScope.cached("u1")).thenReturn(scope("ACTIVE"));
        when(teamScope.live("u1")).thenReturn(scope("DISABLED"));

        mockMvc.perform(post("/test/notes").with(user("notes:write"))).andExpect(status().isForbidden());
    }

    @Test
    void activeUserMayWrite() throws Exception {
        when(teamScope.live("u1")).thenReturn(scope("ACTIVE"));

        mockMvc.perform(post("/test/notes").with(user("notes:write"))).andExpect(status().isOk());
    }

    @Test
    void readUsesCachedStatus() throws Exception {
        when(teamScope.cached("u1")).thenReturn(scope("ACTIVE"));

        mockMvc.perform(get("/test/notes").with(user("notes:read"))).andExpect(status().isOk());
        verify(teamScope, never()).live("u1");
    }

    @Test
    void inactiveUserGets403OnRead() throws Exception {
        when(teamScope.cached("u1")).thenReturn(scope("DISABLED"));

        mockMvc.perform(get("/test/notes").with(user("notes:read"))).andExpect(status().isForbidden());
    }

    @Test
    void userUnknownToUmsGets403() throws Exception {
        when(teamScope.cached("u1")).thenReturn(TeamScope.unknown("u1"));

        mockMvc.perform(get("/test/notes").with(user("notes:read"))).andExpect(status().isForbidden());
    }

    @Test
    void writeFailsClosedWith503WhenUmsIsDown() throws Exception {
        when(teamScope.live("u1")).thenThrow(new UmsUnavailableException("down", null));

        mockMvc.perform(post("/test/notes").with(user("notes:write"))).andExpect(status().isServiceUnavailable());
    }

    private static TeamScope scope(String status) {
        return new TeamScope("u1", status, Set.of(), Set.of(), false);
    }

    private static RequestPostProcessor user(String permission) {
        return jwt().jwt(token -> token.subject("u1")).authorities(new SimpleGrantedAuthority(permission));
    }

    @RestController
    static class NoteController {

        @GetMapping("/test/notes")
        @RequiresPermission(value = "notes:read", description = "Read notes")
        String list() {
            return "notes";
        }

        @PostMapping("/test/notes")
        @RequiresPermission(value = "notes:write", description = "Change notes")
        String create() {
            return "created";
        }
    }
}
