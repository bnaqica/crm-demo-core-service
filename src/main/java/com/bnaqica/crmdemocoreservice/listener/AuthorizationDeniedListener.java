package com.bnaqica.crmdemocoreservice.listener;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.service.AuthorizationAuditService;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Audits endpoints refused for a missing {@link RequiresPermission}. Requests without a token are 401s, not
 * authorization denials, and are left out.
 */
@Component
@RequiredArgsConstructor
public class AuthorizationDeniedListener {

    private final AuthorizationAuditService audit;

    @EventListener
    void on(AuthorizationDeniedEvent<?> event) {
        if (event.getSource() instanceof MethodInvocation invocation) {
            RequiresPermission declared =
                    AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), RequiresPermission.class);
            if (declared != null) {
                audit.denied(declared.value(), "missing permission");
            }
        }
    }
}
