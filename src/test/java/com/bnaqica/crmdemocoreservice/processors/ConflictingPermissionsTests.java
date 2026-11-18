package com.bnaqica.crmdemocoreservice.processors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(useDefaultFilters = false)
@Import({
    DeclaredPermissions.class,
    DeclaredPermissionsTests.SubmissionController.class,
    ConflictingPermissionsTests.ConflictingController.class
})
class ConflictingPermissionsTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DeclaredPermissions declared;

    @Test
    void catalogRefusesDisagreeingDeclarations() {
        assertThatThrownBy(declared::catalog)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("submissions:read");
    }

    @RestController
    static class ConflictingController {

        @GetMapping("/test/other")
        @RequiresPermission(value = "submissions:read", description = "Something else")
        String list() {
            return "";
        }
    }
}
