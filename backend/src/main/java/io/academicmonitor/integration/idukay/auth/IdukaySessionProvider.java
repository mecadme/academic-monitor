package io.academicmonitor.integration.idukay.auth;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import java.util.UUID;

public interface IdukaySessionProvider {

    IdukayAuthenticatedSession getSession(AcademicPlatformContext context);

    /**
     * Returns the sole authenticated session for an institution. This is only intended for
     * development transport checks that must not accept a teacher identifier from HTTP.
     */
    IdukayAuthenticatedSession getSoleSessionForInstitution(UUID institutionId);
}
