package com.bnaqica.crmdemocoreservice.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** The caller of the current request. */
public final class CurrentUser {

    private CurrentUser() {}

    /** The caller's token; empty without one (outside requests, or in the {@code local} profile). */
    public static Optional<Jwt> token() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt
                ? Optional.of(jwt.getToken())
                : Optional.empty();
    }

    /** @throws org.springframework.security.access.AccessDeniedException without a token */
    public static String sub() {
        return token().map(Jwt::getSubject)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No signed-in user"));
    }

    public static Optional<HttpServletRequest> request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? Optional.of(attributes.getRequest())
                : Optional.empty();
    }
}
