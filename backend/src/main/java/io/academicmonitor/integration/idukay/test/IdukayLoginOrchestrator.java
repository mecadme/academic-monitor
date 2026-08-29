package io.academicmonitor.integration.idukay.test;

import io.academicmonitor.integration.idukay.auth.IdukayAuthClient;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukayLoginContexts;
import io.academicmonitor.integration.idukay.auth.IdukayLoginProfile;
import io.academicmonitor.integration.idukay.auth.IdukayLoginProfiles;
import io.academicmonitor.integration.idukay.auth.IdukayLoginSession;
import io.academicmonitor.integration.idukay.auth.IdukayOauthProfile;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IdukayLoginOrchestrator {

    private final IdukayAuthClient authClient;
    private final IdukayTeacherCoursesClient coursesClient;

    public IdukayLoginOrchestrator(IdukayAuthClient authClient, IdukayTeacherCoursesClient coursesClient) {

        this.authClient = authClient;
        this.coursesClient = coursesClient;
    }

    public IdukayTestLoginResponse testLogin(IdukayTestLoginRequest request) {

        validateRequest(request);

        char[] workingPassword = request.password().clone();

        try {
            IdukayLoginSession loginSession =
                    authClient.startLogin(request.email(), workingPassword, request.subdomainSchool());

            IdukayLoginContexts contexts = authClient.getAvailableContexts(loginSession);

            ProfileSelection selection =
                    resolveProfile(loginSession, contexts, request.schoolId(), request.profileId());

            if (selection == null) {
                return selectionRequired(loginSession, contexts, request.schoolId());
            }

            IdukayOauthProfile oauthProfile = IdukayOauthProfile.from(selection.profile(), selection.userId());

            IdukayAuthenticatedSession authenticatedSession =
                    authClient.completeLogin(loginSession, oauthProfile, request.fingerprint());

            List<IdukayTeacherCourseDto> courses = coursesClient.findTeacherCourses(authenticatedSession);

            return IdukayTestLoginResponse.authenticated(
                    authenticatedSession.context().profileType(), courses.size());

        } finally {
            Arrays.fill(workingPassword, '\0');

            Arrays.fill(request.password(), '\0');
        }
    }

    private ProfileSelection resolveProfile(
            IdukayLoginSession loginSession,
            IdukayLoginContexts contexts,
            String requestedSchoolId,
            String requestedProfileId) {

        String contextUserId = contexts.user() != null && contexts.user().isTextual()
                ? contexts.user().asText()
                : null;

        if (!contexts.profiles().isEmpty() && contextUserId != null) {

            IdukayLoginProfile profile = selectProfile(contexts.profiles(), requestedProfileId);

            if (profile != null) {
                return new ProfileSelection(contextUserId, profile);
            }
        }

        if (requestedSchoolId == null || requestedSchoolId.isBlank()) {

            return null;
        }

        IdukayLoginProfiles schoolProfiles = authClient.getProfilesBySchool(loginSession, requestedSchoolId);

        IdukayLoginProfile profile = selectProfile(schoolProfiles.profiles(), requestedProfileId);

        if (profile == null) {
            return null;
        }

        return new ProfileSelection(schoolProfiles.user(), profile);
    }

    private IdukayTestLoginResponse selectionRequired(
            IdukayLoginSession loginSession, IdukayLoginContexts contexts, String requestedSchoolId) {

        List<IdukayTestLoginResponse.SchoolOption> schools = contexts.schools().stream()
                .map(school -> new IdukayTestLoginResponse.SchoolOption(school.id(), school.name()))
                .toList();

        List<IdukayLoginProfile> availableProfiles = contexts.profiles();

        if (availableProfiles.isEmpty() && requestedSchoolId != null && !requestedSchoolId.isBlank()) {

            availableProfiles = authClient
                    .getProfilesBySchool(loginSession, requestedSchoolId)
                    .profiles();
        }

        List<IdukayTestLoginResponse.ProfileOption> profiles = availableProfiles.stream()
                .map(profile -> new IdukayTestLoginResponse.ProfileOption(profile.id(), profile.collectionName()))
                .toList();

        return IdukayTestLoginResponse.selectionRequired(schools, profiles);
    }

    private static IdukayLoginProfile selectProfile(List<IdukayLoginProfile> profiles, String requestedProfileId) {

        if (requestedProfileId != null && !requestedProfileId.isBlank()) {

            return profiles.stream()
                    .filter(profile -> requestedProfileId.equals(profile.id()))
                    .findFirst()
                    .orElse(null);
        }

        List<IdukayLoginProfile> staffProfiles = profiles.stream()
                .filter(profile -> "staff".equals(profile.collectionName()))
                .toList();

        if (staffProfiles.size() == 1) {
            return staffProfiles.getFirst();
        }

        if (profiles.size() == 1) {
            return profiles.getFirst();
        }

        return null;
    }

    private static void validateRequest(IdukayTestLoginRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        if (request.email() == null || request.email().isBlank()) {

            throw new IllegalArgumentException("email is required");
        }

        if (request.password() == null || request.password().length == 0) {

            throw new IllegalArgumentException("password is required");
        }

        if (request.fingerprint() == null) {
            throw new IllegalArgumentException("fingerprint is required");
        }
    }

    private record ProfileSelection(String userId, IdukayLoginProfile profile) {}
}
