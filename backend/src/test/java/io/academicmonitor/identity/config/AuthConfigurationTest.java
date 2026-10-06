package io.academicmonitor.identity.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthConfigurationTest {
    private final AuthSecurityConfiguration configuration = new AuthSecurityConfiguration();

    @Test
    void rejectsAbsentMalformedAndShortSecretsWithoutExposingThem() {
        for (String secret :
                new String[] {null, "", "not-base64!", Base64.getEncoder().encodeToString(new byte[31])}) {
            assertThrows(
                    IllegalStateException.class,
                    () -> configuration.jwtSigningKey(new AuthProperties(null, null, secret, true, "Lax")));
        }
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        assertEquals(
                32,
                configuration
                        .jwtSigningKey(new AuthProperties(
                                null, null, Base64.getEncoder().encodeToString(key), true, "Lax"))
                        .getEncoded()
                        .length);
    }

    @Test
    void defaultsAreFifteenMinutesSevenDaysAndSecureLaxCookies() {
        var properties = new AuthProperties(null, null, null, null, null);
        assertEquals(Duration.ofMinutes(15), properties.accessTtl());
        assertEquals(Duration.ofDays(7), properties.refreshTtl());
        assertTrue(properties.cookieSecure());
        assertEquals("Lax", properties.cookieSameSite());
        assertThrows(IllegalArgumentException.class, () -> new AuthProperties(Duration.ZERO, null, null, true, "Lax"));
        assertThrows(IllegalArgumentException.class, () -> new AuthProperties(null, null, null, false, "None"));
    }

    @Test
    void forbidsInsecureCookiesOutsideDevAndTest() {
        var properties = new AuthProperties(null, null, null, false, "Lax");
        assertThrows(
                IllegalStateException.class,
                () -> configuration.csrfTokenRepository(properties, new MockEnvironment()));
        var dev = new MockEnvironment();
        dev.setActiveProfiles("dev");
        assertNotNull(configuration.csrfTokenRepository(properties, dev));
    }
}
