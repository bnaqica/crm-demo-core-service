package com.bnaqica.crmdemocoreservice.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The permission ({@code <resource>:read} or {@code <resource>:write}) a caller's token must hold to call the
 * endpoint; otherwise the response is 403. A {@code :write} permission also satisfies {@code :read}.
 *
 * <p>Every endpoint declares one; a test fails the build otherwise. The declared permissions are registered with the
 * UMS catalog at startup, with {@link #description()} and {@link #sensitive()}; endpoints sharing a permission must
 * agree on both.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasPermission(null, '{value}')") // see CrmPermissionEvaluator
public @interface RequiresPermission {

    String value();

    /** Shown to admins in the UMS catalog. */
    String description();

    /** Sensitive permissions are left out of Super Manager unless an admin adds them. */
    boolean sensitive() default false;
}
