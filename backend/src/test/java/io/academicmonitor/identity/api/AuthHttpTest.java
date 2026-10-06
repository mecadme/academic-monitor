package io.academicmonitor.identity.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.academicmonitor.identity.application.*;
import io.academicmonitor.identity.config.AuthProperties;
import io.academicmonitor.identity.config.AuthSecurityConfiguration;
import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.shared.config.CorsConfiguration;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ActiveProfiles("test")
@ContextConfiguration(
        classes = {
            AuthSecurityConfiguration.class,
            AuthCookies.class,
            AuthController.class,
            AuthenticatedAcademicContext.class,
            CorsConfiguration.class,
            AuthHttpTest.TestBeans.class
        })
@TestPropertySource(
        properties = {
            "app.auth.jwt-secret-base64=dGVzdC1vbmx5LWtleS0wMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
            "app.auth.cookie-secure=false",
            "app.cors.allowed-origins=http://localhost:3000"
        })
class AuthHttpTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final AcademicMonitorPrincipal PRINCIPAL =
            new AcademicMonitorPrincipal(USER, INSTITUTION, SystemRole.USER, InstitutionRole.TEACHER);
    private static final AuthView VIEW = new AuthView(
            new AuthView.UserView(USER, "teacher@example.com", SystemRole.USER),
            new AuthView.InstitutionView(INSTITUTION, "School", InstitutionRole.TEACHER));

    @Autowired
    private WebApplicationContext application;

    @Autowired
    private AuthenticationService service;

    @Autowired
    private AccessTokenService access;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private JwtEncoder encoder;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(application)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        when(service.login(eq("teacher@example.com"), eq("test-password"), isNull()))
                .thenAnswer(invocation -> session());
        when(service.me(PRINCIPAL)).thenReturn(VIEW);
    }

    @Test
    void csrfEndpointInitializesReadableCookieWithoutAuthenticationOrHttpSession() throws Exception {
        var result = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        var csrf = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrf);
        assertFalse(csrf.isHttpOnly());
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void validLoginIssuesSignedTokensOnlyAsHttpOnlyCookiesAndExpectedClaims() throws Exception {
        var result = mvc.perform(csrf(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teacher@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();
        Cookie accessCookie = result.getResponse().getCookie("am_access");
        Cookie refreshCookie = result.getResponse().getCookie("am_refresh");
        assertNotNull(accessCookie);
        assertNotNull(refreshCookie);
        assertTrue(accessCookie.isHttpOnly());
        assertTrue(refreshCookie.isHttpOnly());
        assertFalse(accessCookie.getSecure());
        assertFalse(refreshCookie.getSecure());
        assertEquals("/", accessCookie.getPath());
        assertEquals("/api/v1/auth", refreshCookie.getPath());
        assertEquals(900, accessCookie.getMaxAge());
        assertEquals(7 * 24 * 60 * 60, refreshCookie.getMaxAge());
        assertEquals("Lax", accessCookie.getAttribute("SameSite"));
        assertNull(result.getRequest().getSession(false));
        var jwt = decoder.decode(accessCookie.getValue());
        assertEquals(USER.toString(), jwt.getSubject());
        assertEquals(INSTITUTION.toString(), jwt.getClaimAsString("institutionId"));
        assertEquals("USER", jwt.getClaimAsString("systemRole"));
        assertEquals("TEACHER", jwt.getClaimAsString("institutionRole"));
        assertEquals("academic-monitor", jwt.getClaimAsString("iss"));
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getId());
        assertEquals(
                900,
                java.time.Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())
                        .toSeconds());
        assertEquals(
                java.util.Set.of("iss", "sub", "institutionId", "systemRole", "institutionRole", "iat", "exp", "jti"),
                jwt.getClaims().keySet());
    }

    @Test
    void invalidCredentialsReturnGeneric401AndNeverExposeInputs() throws Exception {
        when(service.login(anyString(), anyString(), any())).thenThrow(new AuthFailure());
        for (String email : List.of("teacher@example.com", "missing@example.com")) {
            mvc.perform(csrf(post("/api/v1/auth/login"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"incorrect-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
        }
    }

    @Test
    void invalidLoginPayloadReturnsGeneric400WithoutReflectingPassword() throws Exception {
        String rejectedPassword = "sensitive-value-" + "x".repeat(1024);
        var result = mvc.perform(csrf(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teacher@example.com\",\"password\":\"" + rejectedPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN_REQUEST"))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains(rejectedPassword));
        verifyNoInteractions(service);
    }

    @Test
    void loginNormalizesSurroundingEmailWhitespaceBeforeValidation() throws Exception {
        mvc.perform(csrf(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  teacher@example.com  \",\"password\":\"test-password\"}"))
                .andExpect(status().isOk());

        verify(service).login("teacher@example.com", "test-password", null);
    }

    @Test
    void selectionRequiredReturnsSafeInstitutionChoicesWithoutTokens() throws Exception {
        when(service.login(anyString(), anyString(), any()))
                .thenThrow(new InstitutionSelectionRequired(List.of(new InstitutionSelectionRequired.InstitutionChoice(
                        INSTITUTION, "School", InstitutionRole.TEACHER))));
        var result = mvc.perform(csrf(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teacher@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSTITUTION_SELECTION_REQUIRED"))
                .andExpect(jsonPath("$.institutions[0].institutionId").value(INSTITUTION.toString()))
                .andReturn();
        assertNull(result.getResponse().getCookie("am_access"));
        assertNull(result.getResponse().getCookie("am_refresh"));
    }

    @Test
    void meRequiresSignedUnexpiredAuthenticationAndReturnsCurrentPrincipal() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("am_access", access.issue(PRINCIPAL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(USER.toString()));
        verify(service).me(PRINCIPAL);
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("am_access", expired())))
                .andExpect(status().isUnauthorized());
        String jwt = access.issue(PRINCIPAL);
        int index = jwt.lastIndexOf('.') + 1;
        String tampered = jwt.substring(0, index) + (jwt.charAt(index) == 'A' ? 'B' : 'A') + jwt.substring(index + 1);
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("am_access", tampered)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("JSESSIONID", "session-cookie-cannot-authenticate")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfProtectsLoginRefreshLogoutAndAuthenticatedPostPatchDelete() throws Exception {
        for (String endpoint : List.of("login", "refresh", "logout")) {
            mvc.perform(post("/api/v1/auth/" + endpoint)).andExpect(status().isForbidden());
        }
        for (var builder : List.of(
                post("/api/v1/security-test"), patch("/api/v1/security-test"), delete("/api/v1/security-test"))) {
            mvc.perform(builder.cookie(new Cookie("am_access", access.issue(PRINCIPAL))))
                    .andExpect(status().isForbidden());
        }
        for (var builder : List.of(
                post("/api/v1/security-test"), patch("/api/v1/security-test"), delete("/api/v1/security-test"))) {
            mvc.perform(csrf(builder).cookie(new Cookie("am_access", access.issue(PRINCIPAL))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void teacherApisDenyAdministrativeMembershipWith403() throws Exception {
        var admin = new AcademicMonitorPrincipal(USER, INSTITUTION, SystemRole.USER, InstitutionRole.ADMIN);
        mvc.perform(csrf(post("/api/v1/security-test")).cookie(new Cookie("am_access", access.issue(admin))))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshAndLogoutWorkWithExpiredAccessCookieAndLogoutClearsBothCookies() throws Exception {
        when(service.refresh("refresh-cookie")).thenAnswer(invocation -> session());
        mvc.perform(csrf(post("/api/v1/auth/refresh"))
                        .cookie(new Cookie("am_access", expired()), new Cookie("am_refresh", "refresh-cookie")))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("am_access", true));
        mvc.perform(csrf(post("/api/v1/auth/logout"))
                        .cookie(new Cookie("am_access", expired()), new Cookie("am_refresh", "refresh-cookie")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("am_access", 0))
                .andExpect(cookie().maxAge("am_refresh", 0));
        verify(service).logout("refresh-cookie");
    }

    @Test
    void rejectedRefreshReturns401AndClearsAuthenticationCookies() throws Exception {
        when(service.refresh(any())).thenThrow(new AuthFailure());
        mvc.perform(csrf(post("/api/v1/auth/refresh")).cookie(new Cookie("am_refresh", "revoked-refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge("am_access", 0))
                .andExpect(cookie().maxAge("am_refresh", 0));
    }

    @Test
    void productionCookieConfigurationSetsSecureOnBothCookies() {
        var cookies = new AuthCookies(new AuthProperties(null, null, null, true, "Lax"));
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        cookies.set(response, session());
        assertTrue(response.getCookie("am_access").getSecure());
        assertTrue(response.getCookie("am_refresh").getSecure());
    }

    @Test
    void corsAllowsOnlyConfiguredOriginWithCredentials() throws Exception {
        mvc.perform(options("/api/v1/auth/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/v1/auth/me")
                        .header("Origin", "https://unexpected.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapIsRemovedAndProductionSwaggerIsInaccessible() throws Exception {
        mvc.perform(csrf(post("/api/v1/context/bootstrap")).cookie(new Cookie("am_access", access.issue(PRINCIPAL))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html").cookie(new Cookie("am_access", access.issue(PRINCIPAL))))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder builder) throws Exception {
        var response = mvc.perform(get("/api/v1/auth/csrf")).andReturn().getResponse();
        Cookie token = response.getCookie("XSRF-TOKEN");
        assertNotNull(token);
        return builder.cookie(token).header("X-XSRF-TOKEN", token.getValue());
    }

    private AuthenticationService.AuthSession session() {
        return new AuthenticationService.AuthSession(
                VIEW, access.issue(PRINCIPAL), "opaque-refresh-test-token", UUID.randomUUID());
    }

    private String expired() {
        var claims = JwtClaimsSet.builder()
                .subject(USER.toString())
                .issuer("academic-monitor")
                .claim("institutionId", INSTITUTION.toString())
                .claim("systemRole", "USER")
                .claim("institutionRole", "TEACHER")
                .issuedAt(Instant.now().minusSeconds(120))
                .expiresAt(Instant.now().minusSeconds(60))
                .id(UUID.randomUUID().toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class TestBeans {
        @Bean
        AuthenticationService authenticationService() {
            return mock(AuthenticationService.class);
        }

        @Bean
        AccessTokenService accessTokenService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
            return new AccessTokenService(encoder, properties, clock);
        }

        @Bean
        MutableEndpoint mutableEndpoint() {
            return new MutableEndpoint();
        }
    }

    @RestController
    static class MutableEndpoint {
        @RequestMapping(
                path = "/api/v1/security-test",
                method = {RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE})
        public String mutate() {
            return "ok";
        }
    }
}
