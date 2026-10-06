package io.academicmonitor.integration.idukay.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.identity.application.AcademicMonitorPrincipal;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.integration.idukay.auth.*;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

class IdukayMultiuserIsolationTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void twoAuthenticatedTeachersConnectIntoSeparateSessionsWithinTheSameInstitution() {
        UUID institution = UUID.randomUUID();
        UUID teacherA = UUID.randomUUID();
        UUID teacherB = UUID.randomUUID();
        var contextA = new AcademicPlatformContext(institution, teacherA);
        var contextB = new AcademicPlatformContext(institution, teacherB);
        var sessions = new InMemoryIdukaySessionProvider();
        var auth = mock(IdukayAuthClient.class);
        var courses = mock(IdukayTeacherCoursesClient.class);
        var loginA = mock(IdukayLoginSession.class);
        var loginB = mock(IdukayLoginSession.class);
        var sessionA = mock(IdukayAuthenticatedSession.class);
        var sessionB = mock(IdukayAuthenticatedSession.class);
        var providerContext = new IdukaySessionContext(
                "year", "school", "organization", null, "profile", "staff", "America/Guayaquil", null);
        when(sessionA.context()).thenReturn(providerContext);
        when(sessionB.context()).thenReturn(providerContext);
        when(auth.startLogin(eq("idukay-a@example.test"), any(char[].class), isNull()))
                .thenReturn(loginA);
        when(auth.startLogin(eq("idukay-b@example.test"), any(char[].class), isNull()))
                .thenReturn(loginB);
        var available = new IdukayLoginContexts(
                JsonMapper.builder().build().readTree("\"provider-user\""),
                List.of(),
                null,
                false,
                List.of(new IdukayLoginProfile("profile", "staff")));
        when(auth.getAvailableContexts(loginA)).thenReturn(available);
        when(auth.getAvailableContexts(loginB)).thenReturn(available);
        when(auth.completeLogin(eq(loginA), any(), any())).thenReturn(sessionA);
        when(auth.completeLogin(eq(loginB), any(), any())).thenReturn(sessionB);
        var orchestrator = new IdukayLoginOrchestrator(auth, courses, sessions, new AuthenticatedAcademicContext());

        authenticate(institution, teacherA);
        orchestrator.testLogin(request("idukay-a@example.test"));
        assertSame(sessionA, sessions.getSession(contextA));
        assertThrows(IllegalStateException.class, () -> sessions.getSession(contextB));

        authenticate(institution, teacherB);
        orchestrator.testLogin(request("idukay-b@example.test"));
        assertSame(sessionB, sessions.getSession(contextB));
        assertSame(sessionA, sessions.getSession(contextA));
        assertNotSame(sessions.getSession(contextA), sessions.getSession(contextB));
        assertThrows(
                IllegalStateException.class,
                () -> sessions.getSession(new AcademicPlatformContext(UUID.randomUUID(), teacherA)));
    }

    private void authenticate(UUID institutionId, UUID userId) {
        var principal = new AcademicMonitorPrincipal(userId, institutionId, SystemRole.USER, InstitutionRole.TEACHER);
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    private IdukayTestLoginRequest request(String email) {
        var fingerprint = new IdukayFingerprint(
                "test-agent", "es", List.of("es"), "test", 1, 1d, Map.of(), "America/Guayaquil", 0, null, null, null);
        return new IdukayTestLoginRequest(email, "temporary-test-input".toCharArray(), null, null, null, fingerprint);
    }
}
