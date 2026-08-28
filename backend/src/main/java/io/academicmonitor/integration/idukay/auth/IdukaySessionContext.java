package io.academicmonitor.integration.idukay.auth;

public record IdukaySessionContext(
        String workingYear,
        String workingSchool,
        String workingOrganization,
        String selectedStudent,
        String workingProfile,
        String profileType,
        String timeZone,
        String acceptedPermissions) {}
