package com.bnaqica.crmdemocoreservice.processors;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test controllers are nested in a class with {@code @Test} methods, so full-application tests don't scan them. */
@WebMvcTest(useDefaultFilters = false)
@Import({
    DeclaredPermissions.class,
    DeclaredPermissionsTests.UnannotatedController.class,
    DeclaredPermissionsTests.SubmissionController.class
})
class DeclaredPermissionsTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DeclaredPermissions declared;

    @Test
    void namesTheEndpointWithoutAPermission() {
        assertThat(declared.undeclaredEndpoints())
                .containsExactly("GET [/test/unannotated] " + UnannotatedController.class.getName() + "#list");
    }

    @Test
    void catalogListsEachDeclaredPermissionOnce() {
        assertThat(declared.catalog())
                .containsExactlyInAnyOrder(
                        new Permission("submissions:read", "crm-internal", "Read portal submissions", false),
                        new Permission("submissions:write", "crm-internal", "Decide portal submissions", true));
    }

    @RestController
    static class UnannotatedController {

        @GetMapping("/test/unannotated")
        String list() {
            return "";
        }
    }

    @RestController
    static class SubmissionController {

        @GetMapping("/test/submissions")
        @RequiresPermission(value = "submissions:read", description = "Read portal submissions")
        String list() {
            return "";
        }

        @GetMapping("/test/submissions/one")
        @RequiresPermission(value = "submissions:read", description = "Read portal submissions")
        String one() {
            return "";
        }

        @PostMapping("/test/submissions")
        @RequiresPermission(value = "submissions:write", description = "Decide portal submissions", sensitive = true)
        String decide() {
            return "";
        }
    }
}
