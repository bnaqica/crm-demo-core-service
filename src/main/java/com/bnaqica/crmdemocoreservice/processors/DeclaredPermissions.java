package com.bnaqica.crmdemocoreservice.processors;

import com.bnaqica.crmdemocoreservice.config.AnyActiveInternalUser;
import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration.Permission;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The {@link RequiresPermission} declarations on this service's endpoints, read from the registered handler methods.
 * Endpoints from libraries (such as Spring's error controller) aren't this service's and are ignored.
 */
@Component
public class DeclaredPermissions {

    static final String REALM = "crm-internal";

    /** The only handlers allowed to use {@link AnyActiveInternalUser}; adding one needs the developer's agreement. */
    static final Set<String> ANY_ACTIVE_USER_ALLOWLIST =
            Set.of("com.bnaqica.crmdemocoreservice.api.CapabilitiesController#capabilities");

    private static final String BASE_PACKAGE = "com.bnaqica.crmdemocoreservice.";

    private final RequestMappingHandlerMapping handlerMapping;

    public DeclaredPermissions(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    /**
     * Each endpoint without a permission, as {@code GET [/path] Controller#method}, sorted. An
     * {@link AnyActiveInternalUser} endpoint counts as declared only if it is on the allowlist.
     */
    public List<String> undeclaredEndpoints() {
        return ownHandlers().entrySet().stream()
                .map(entry -> problem(entry.getKey(), entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
    }

    private static String problem(RequestMappingInfo info, HandlerMethod handler) {
        if (permission(handler) != null) {
            return null;
        }
        if (!handler.hasMethodAnnotation(AnyActiveInternalUser.class)) {
            return describe(info, handler);
        }
        String name =
                handler.getBeanType().getName() + "#" + handler.getMethod().getName();
        return ANY_ACTIVE_USER_ALLOWLIST.contains(name)
                ? null
                : describe(info, handler) + " (@AnyActiveInternalUser is only for allowlisted handlers)";
    }

    /**
     * Every declared permission once, sorted by key.
     *
     * @throws IllegalStateException if endpoints declaring the same permission disagree on description or sensitivity
     */
    public List<Permission> catalog() {
        Map<String, Set<Permission>> byKey = ownHandlers().values().stream()
                .map(DeclaredPermissions::permission)
                .filter(java.util.Objects::nonNull)
                .map(declared -> new Permission(declared.value(), REALM, declared.description(), declared.sensitive()))
                .collect(Collectors.groupingBy(Permission::key, TreeMap::new, Collectors.toSet()));
        byKey.forEach((key, variants) -> {
            if (variants.size() > 1) {
                throw new IllegalStateException(
                        "Endpoints declaring " + key + " disagree on description or sensitivity: " + variants);
            }
        });
        return byKey.values().stream()
                .map(variants -> variants.iterator().next())
                .sorted(Comparator.comparing(Permission::key))
                .toList();
    }

    private Map<RequestMappingInfo, HandlerMethod> ownHandlers() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getName().startsWith(BASE_PACKAGE))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static RequiresPermission permission(HandlerMethod handler) {
        return AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), RequiresPermission.class);
    }

    private static String describe(RequestMappingInfo info, HandlerMethod handler) {
        var methods = info.getMethodsCondition().getMethods();
        String verbs = methods.isEmpty()
                ? "ANY"
                : methods.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        return verbs + " " + info.getPatternValues() + " "
                + handler.getBeanType().getName() + "#" + handler.getMethod().getName();
    }
}
