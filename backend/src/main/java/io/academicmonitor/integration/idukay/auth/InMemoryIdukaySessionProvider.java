package io.academicmonitor.integration.idukay.auth;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryIdukaySessionProvider implements IdukaySessionProvider {

    private final Map<AcademicPlatformContext, IdukayAuthenticatedSession> sessions =
        new ConcurrentHashMap<>();

    @Override
    public IdukayAuthenticatedSession getSession(
        AcademicPlatformContext context) {

        IdukayAuthenticatedSession session =
            sessions.get(context);

        if (session == null) {
            throw new IllegalStateException(
                "No authenticated Idukay session is available for the requested context");
        }

        return session;
    }

    public void storeSession(
        AcademicPlatformContext context,
        IdukayAuthenticatedSession session) {

        if (context == null) {
            throw new IllegalArgumentException(
                "context is required");
        }

        if (session == null) {
            throw new IllegalArgumentException(
                "session is required");
        }

        sessions.put(
            context,
            session);
    }
}
