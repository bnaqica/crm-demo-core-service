package com.bnaqica.crmdemocoreservice.processors;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.InactiveUserException;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import com.bnaqica.crmdemocoreservice.service.TeamScopeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Lets only users whose UMS status is ACTIVE reach an endpoint, so a user disabled after their token was issued is
 * stopped at once. Reads check the cached status; writes check the live status, and fail with 503 if the UMS can't
 * answer. Runs before the endpoint's permission check; requests without a token (only possible in the {@code local}
 * profile) pass. Denials are audited (and logged by the audit service).
 */
@Component
@RequiredArgsConstructor
public class ActiveUserInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final TeamScopeService teamScope;
    private final AuthorizationAuditService audit;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)
                || !(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt)) {
            return true;
        }
        String sub = jwt.getToken().getSubject();
        TeamScope scope = READ_METHODS.contains(request.getMethod()) ? teamScope.cached(sub) : teamScope.live(sub);
        if (!scope.isActive()) {
            RequiresPermission declared = method.getMethodAnnotation(RequiresPermission.class);
            audit.denied(declared == null ? null : declared.value(), "user status " + scope.status());
            throw new InactiveUserException(sub, scope.status());
        }
        return true;
    }
}
