package com.bnaqica.crmdemocoreservice.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declares an endpoint open to every ACTIVE internal user, whatever permissions they hold: the {@code crm-internal}
 * token with audience {@code crm-api} and the ACTIVE check still apply. It is the one exception to
 * {@link RequiresPermission}, isn't registered in the catalog, and only handlers on the allowlist in
 * {@code DeclaredPermissions} may use it (today only {@code GET /me/capabilities}); on any other handler the
 * architecture test fails the build.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("isAuthenticated()")
public @interface AnyActiveInternalUser {}
