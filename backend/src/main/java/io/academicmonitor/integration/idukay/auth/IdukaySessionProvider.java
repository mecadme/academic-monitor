package io.academicmonitor.integration.idukay.auth;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;

public interface IdukaySessionProvider {

    IdukayAuthenticatedSession getSession(AcademicPlatformContext context);
}
