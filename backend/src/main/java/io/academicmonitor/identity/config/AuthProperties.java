package io.academicmonitor.identity.config;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        Duration accessTtl, Duration refreshTtl, String jwtSecretBase64, Boolean cookieSecure, String cookieSameSite) {
    public AuthProperties {
        accessTtl = accessTtl == null ? Duration.ofMinutes(15) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(7) : refreshTtl;
        cookieSecure = cookieSecure == null ? true : cookieSecure;
        cookieSameSite = cookieSameSite == null ? "Lax" : cookieSameSite;
        if (accessTtl.isNegative() || accessTtl.isZero() || refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("Authentication token lifetimes must be positive");
        }
        if (!Set.of("Lax", "Strict", "None").contains(cookieSameSite)) {
            throw new IllegalArgumentException("Authentication cookie SameSite must be Lax, Strict or None");
        }
        if ("None".equals(cookieSameSite) && !cookieSecure) {
            throw new IllegalArgumentException("SameSite=None requires Secure authentication cookies");
        }
    }

    @Override
    public String toString() {
        return "AuthProperties[credentials redacted]";
    }
}
