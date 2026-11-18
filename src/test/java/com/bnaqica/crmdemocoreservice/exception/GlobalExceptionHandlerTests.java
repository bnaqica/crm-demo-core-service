package com.bnaqica.crmdemocoreservice.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocoreservice.config.SecurityConfig;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(useDefaultFilters = false)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, GlobalExceptionHandlerTests.RacingController.class})
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void concurrentChangeAtCommitIsAConflict() throws Exception {
        mockMvc.perform(post("/test/race").with(jwt())).andExpect(status().isConflict());
    }

    @RestController
    static class RacingController {

        /** What the transaction proxy throws when another request committed the same row first. */
        @PostMapping("/test/race")
        String race() {
            throw new ObjectOptimisticLockingFailureException(Submission.class, "id");
        }
    }
}
