package com.bnaqica.crmdemocoreservice.listener;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.config.SecurityConfig;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(useDefaultFilters = false)
@Import({
    SecurityConfig.class,
    AuthorizationDeniedListener.class,
    AuthorizationDeniedListenerTests.WidgetController.class
})
class AuthorizationDeniedListenerTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthorizationAuditService audit;

    AuthorizationDeniedListenerTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void missingPermissionIsAudited() throws Exception {
        mockMvc.perform(post("/test/widgets").with(jwt().authorities(new SimpleGrantedAuthority("widgets:read"))))
                .andExpect(status().isForbidden());

        verify(audit).denied("widgets:write", "missing permission");
    }

    @Test
    void grantedRequestIsNotAudited() throws Exception {
        mockMvc.perform(post("/test/widgets").with(jwt().authorities(new SimpleGrantedAuthority("widgets:write"))))
                .andExpect(status().isOk());

        verify(audit, never()).denied(anyString(), anyString());
    }

    @Test
    void requestWithoutTokenIsNotAnAuthorizationDenial() throws Exception {
        mockMvc.perform(get("/test/widgets")).andExpect(status().isUnauthorized());

        verify(audit, never()).denied(anyString(), anyString());
    }

    @RestController
    static class WidgetController {

        @PostMapping("/test/widgets")
        @RequiresPermission(value = "widgets:write", description = "Change widgets")
        String create() {
            return "created";
        }
    }
}
