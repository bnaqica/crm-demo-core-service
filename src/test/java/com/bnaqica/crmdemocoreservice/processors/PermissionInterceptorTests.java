package com.bnaqica.crmdemocoreservice.processors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.config.SecurityConfig;
import com.bnaqica.crmdemocoreservice.config.WebMvcConfig;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.GlobalExceptionHandler;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import com.bnaqica.crmdemocoreservice.service.TeamScopeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The permission is checked before the request body is read and validated. */
@WebMvcTest(useDefaultFilters = false)
@Import({
    SecurityConfig.class,
    WebMvcConfig.class,
    PermissionInterceptor.class,
    ActiveUserInterceptor.class,
    GlobalExceptionHandler.class,
    PermissionInterceptorTests.WidgetController.class
})
class PermissionInterceptorTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TeamScopeService teamScope;

    @MockitoBean
    private AuthorizationAuditService audit;

    PermissionInterceptorTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void active() {
        when(teamScope.live("u1")).thenReturn(new TeamScope("u1", "ACTIVE", Set.of(), Set.of(), false));
    }

    @Test
    void callerWithoutPermissionGets403EvenForAnInvalidBody() throws Exception {
        mockMvc.perform(post("/test/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
                        .with(jwt().jwt(token -> token.subject("u1"))
                                .authorities(new SimpleGrantedAuthority("widgets:read"))))
                .andExpect(status().isForbidden());

        verify(audit).denied("widgets:write", "missing permission");
    }

    @Test
    void callerWithPermissionGetsTheValidationError() throws Exception {
        mockMvc.perform(post("/test/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
                        .with(jwt().jwt(token -> token.subject("u1"))
                                .authorities(new SimpleGrantedAuthority("widgets:write"))))
                .andExpect(status().isBadRequest());

        verify(audit, never()).denied(anyString(), anyString());
    }

    record Widget(@NotBlank String name) {}

    @RestController
    static class WidgetController {

        @PostMapping("/test/widgets")
        @RequiresPermission(value = "widgets:write", description = "Change widgets")
        String create(@Valid @RequestBody Widget widget) {
            return "created";
        }
    }
}
