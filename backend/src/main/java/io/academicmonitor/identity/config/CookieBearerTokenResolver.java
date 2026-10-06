package io.academicmonitor.identity.config;

import io.academicmonitor.identity.api.AuthCookies;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

final class CookieBearerTokenResolver implements BearerTokenResolver {
    private static final Set<String> REFRESH_AUTHENTICATED_OR_PUBLIC =
            Set.of("/api/v1/auth/csrf", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout");

    @Override
    public String resolve(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (REFRESH_AUTHENTICATED_OR_PUBLIC.contains(path) || path.startsWith("/actuator/health")) {
            return null;
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (AuthCookies.ACCESS.equals(cookie.getName())
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
