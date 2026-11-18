package com.bnaqica.crmdemocoreservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.bnaqica.crmdemocoreservice.config.AnyActiveInternalUser;
import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Every endpoint of the running application against every kind of caller. New endpoints are picked up
 * automatically. Roles in the UMS are bundles of permissions, so the callers are described by what their token holds:
 *
 * <ul>
 *   <li>no CRM permissions (a new or UI-only user);
 *   <li>exactly the endpoint's permission;
 *   <li>the {@code :write} of the endpoint's resource (which implies {@code :read});
 *   <li>every permission of another resource (Super-Manager-like, but not for this resource);
 *   <li>a user holding the permission whose UMS account is DISABLED.
 * </ul>
 *
 * "Allowed" means the request got past authorization (any status but 401 and 403); the endpoint may still answer
 * 400 or 404 for the made-up ids and empty bodies used here.
 */
class PermissionMatrixTests extends IntegrationTest {

    private static final String OTHER_RESOURCE = "matrix-other";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyEndpointAllowsExactlyTheCallersItShould() throws Exception {
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
                handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            if (!handler.getBeanType().getPackageName().startsWith("com.bnaqica.crmdemocoreservice")) {
                continue;
            }
            for (RequestMethod method : methods(entry.getKey())) {
                for (String pattern : entry.getKey().getPatternValues()) {
                    checked++;
                    check(method, pattern, handler, wrong);
                }
            }
        }
        assertThat(checked).as("endpoints checked").isPositive();
        assertThat(wrong).as("callers treated wrongly").isEmpty();
    }

    private void check(RequestMethod method, String pattern, HandlerMethod handler, List<String> wrong)
            throws Exception {
        String endpoint = method + " " + pattern;
        RequiresPermission declared = handler.getMethodAnnotation(RequiresPermission.class);
        if (declared == null) {
            assertThat(handler.hasMethodAnnotation(AnyActiveInternalUser.class))
                    .as(endpoint + " declares nothing")
                    .isTrue();
            expect(wrong, endpoint, "no permissions", true, call(method, pattern, "ACTIVE"));
            expect(wrong, endpoint, "disabled user", false, call(method, pattern, "DISABLED"));
            return;
        }
        String permission = declared.value();
        String resource = permission.substring(0, permission.lastIndexOf(':'));
        expect(wrong, endpoint, "no permissions", false, call(method, pattern, "ACTIVE"));
        expect(wrong, endpoint, "exactly " + permission, true, call(method, pattern, "ACTIVE", permission));
        expect(wrong, endpoint, resource + ":write", true, call(method, pattern, "ACTIVE", resource + ":write"));
        expect(
                wrong,
                endpoint,
                "another resource",
                false,
                call(method, pattern, "ACTIVE", OTHER_RESOURCE + ":read", OTHER_RESOURCE + ":write"));
        expect(wrong, endpoint, "disabled holder", false, call(method, pattern, "DISABLED", permission));
    }

    private int call(RequestMethod method, String pattern, String status, String... permissions) throws Exception {
        String sub = "matrix-" + UUID.randomUUID();
        when(ums.teamScope(sub)).thenReturn(new TeamScope(sub, status, Set.of(), Set.of(), false));
        String path = pattern.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        var request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method.name()), path)
                .with(jwt().jwt(token -> token.subject(sub))
                        .authorities(java.util.Arrays.stream(permissions)
                                .map(SimpleGrantedAuthority::new)
                                .toArray(SimpleGrantedAuthority[]::new)));
        if (method == RequestMethod.POST || method == RequestMethod.PUT || method == RequestMethod.PATCH) {
            request.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    private static void expect(List<String> wrong, String endpoint, String caller, boolean allowed, int status) {
        boolean wasAllowed = status != 401 && status != 403;
        if (wasAllowed != allowed) {
            wrong.add(endpoint + " for " + caller + ": got " + status + ", expected " + (allowed ? "allowed" : "403"));
        }
    }

    private static Set<RequestMethod> methods(RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        return methods.isEmpty() ? Set.of(RequestMethod.GET) : methods;
    }
}
