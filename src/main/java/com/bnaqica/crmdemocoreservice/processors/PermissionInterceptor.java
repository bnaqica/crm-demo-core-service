package com.bnaqica.crmdemocoreservice.processors;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import com.bnaqica.crmdemocoreservice.service.Permissions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Checks an endpoint's {@link RequiresPermission} before the request is bound, so a caller without the permission
 * gets 403 (audited), never a 400 that describes the request body. The {@code @PreAuthorize} behind the annotation
 * stays as a second check for any call that doesn't come through Spring MVC.
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final AuthorizationAuditService audit;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequiresPermission declared = method.getMethodAnnotation(RequiresPermission.class);
        if (declared == null) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new AccessDeniedException("No token"); // only in the local profile; answered with 401
        }
        var granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        if (!Permissions.effective(granted).contains(declared.value())) {
            audit.denied(declared.value(), "missing permission");
            throw new AccessDeniedException("Missing permission " + declared.value());
        }
        return true;
    }
}
