package com.bnaqica.crmdemocoreservice.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@Import({SecurityConfig.class, RequiresPermissionTests.WidgetController.class})
class RequiresPermissionTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    RequiresPermissionTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void userWithoutWritePermissionGets403OnWriteEndpoint() throws Exception {
        mockMvc.perform(post("/test/widgets").with(holding("widgets:read"))).andExpect(status().isForbidden());
    }

    @Test
    void userWithWritePermissionMayWrite() throws Exception {
        mockMvc.perform(post("/test/widgets").with(holding("widgets:write"))).andExpect(status().isOk());
    }

    @Test
    void writeHolderMayRead() throws Exception {
        mockMvc.perform(get("/test/widgets").with(holding("widgets:write"))).andExpect(status().isOk());
    }

    @Test
    void writeOnAnotherResourceDoesNotImplyRead() throws Exception {
        mockMvc.perform(get("/test/widgets").with(holding("gadgets:write"))).andExpect(status().isForbidden());
    }

    @Test
    void userWithNoPermissionsGets403() throws Exception {
        mockMvc.perform(get("/test/widgets").with(holding())).andExpect(status().isForbidden());
    }

    private static RequestPostProcessor holding(String... permissions) {
        return jwt().authorities(java.util.Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new));
    }

    @RestController
    static class WidgetController {

        @GetMapping("/test/widgets")
        @RequiresPermission(value = "widgets:read", description = "Read widgets")
        String list() {
            return "widgets";
        }

        @PostMapping("/test/widgets")
        @RequiresPermission(value = "widgets:write", description = "Change widgets")
        String create() {
            return "created";
        }
    }
}
