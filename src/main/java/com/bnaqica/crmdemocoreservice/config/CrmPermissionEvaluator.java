package com.bnaqica.crmdemocoreservice.config;

import com.bnaqica.crmdemocoreservice.service.Permissions;
import java.io.Serializable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Backs {@link RequiresPermission}: the caller's authorities, with {@code :write} implying {@code :read}. */
class CrmPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        var granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Permissions.effective(granted).contains(permission.toString());
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable id, String type, Object permission) {
        return false; // not used: permissions aren't per object
    }
}
