package io.academicmonitor.identity.api;

import io.academicmonitor.identity.application.AuthenticationService.AuthSession;
import io.academicmonitor.identity.config.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookies {
    public static final String ACCESS = "am_access";
    public static final String REFRESH = "am_refresh";
    private final AuthProperties properties;

    public AuthCookies(AuthProperties properties) {
        this.properties = properties;
    }

    public void set(HttpServletResponse response, AuthSession session) {
        cookie(response, ACCESS, session.accessToken(), "/", properties.accessTtl());
        cookie(response, REFRESH, session.refreshToken(), "/api/v1/auth", properties.refreshTtl());
    }

    public void clear(HttpServletResponse response) {
        cookie(response, ACCESS, "", "/", Duration.ZERO);
        cookie(response, REFRESH, "", "/api/v1/auth", Duration.ZERO);
    }

    private void cookie(HttpServletResponse response, String name, String value, String path, Duration age) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(name, value)
                        .httpOnly(true)
                        .secure(properties.cookieSecure())
                        .sameSite(properties.cookieSameSite())
                        .path(path)
                        .maxAge(age)
                        .build()
                        .toString());
    }
}
