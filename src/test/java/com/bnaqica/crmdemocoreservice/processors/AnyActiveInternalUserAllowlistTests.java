package com.bnaqica.crmdemocoreservice.processors;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocoreservice.config.AnyActiveInternalUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(useDefaultFilters = false)
@Import({DeclaredPermissions.class, AnyActiveInternalUserAllowlistTests.ProfileController.class})
class AnyActiveInternalUserAllowlistTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DeclaredPermissions declared;

    @Test
    void markerOnAHandlerOutsideTheAllowlistCountsAsUndeclared() {
        assertThat(declared.undeclaredEndpoints())
                .containsExactly("GET [/test/profile] " + ProfileController.class.getName() + "#profile"
                        + " (@AnyActiveInternalUser is only for allowlisted handlers)");
    }

    @Test
    void markerIsNotRegisteredInTheCatalog() {
        assertThat(declared.catalog()).isEmpty();
    }

    @RestController
    static class ProfileController {

        @GetMapping("/test/profile")
        @AnyActiveInternalUser
        String profile() {
            return "";
        }
    }
}
