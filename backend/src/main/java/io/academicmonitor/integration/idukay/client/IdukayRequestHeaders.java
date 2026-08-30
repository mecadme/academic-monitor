package io.academicmonitor.integration.idukay.client;

import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionContext;
import org.springframework.http.HttpHeaders;

final class IdukayRequestHeaders {

    private IdukayRequestHeaders() {}

    static void apply(HttpHeaders headers, IdukayAuthenticatedSession session, String clientVersion) {

        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        session.applyAuthorization(headers);

        IdukaySessionContext context = session.context();

        setIfPresent(headers, "WorkingYear", context.workingYear());

        setIfPresent(headers, "WorkingSchool", context.workingSchool());

        setIfPresent(headers, "WorkingOrganization", context.workingOrganization());

        setIfPresent(headers, "SelectedStudent", context.selectedStudent());

        setIfPresent(headers, "WorkingProfile", context.workingProfile());

        setIfPresent(headers, "ProfileType", context.profileType());

        setIfPresent(headers, "TimeZone", context.timeZone());

        setIfPresent(headers, "AcceptedPermissions", context.acceptedPermissions());

        setIfPresent(headers, "ClientVersion", clientVersion);
    }

    private static void setIfPresent(HttpHeaders headers, String name, String value) {

        if (value != null && !value.isBlank()) {

            headers.set(name, value.trim());
        }
    }
}
