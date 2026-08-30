package io.academicmonitor.integration.idukay.test;

import java.util.List;

public record IdukayTestLoginResponse(
        String status,
        boolean authenticated,
        String profileType,
        int coursesFound,
        List<SchoolOption> schools,
        List<ProfileOption> profiles) {

    public record SchoolOption(String id, String name) {}

    public record ProfileOption(String id, String type) {}

    public static IdukayTestLoginResponse authenticated(String profileType, int coursesFound) {

        return new IdukayTestLoginResponse("AUTHENTICATED", true, profileType, coursesFound, List.of(), List.of());
    }

    public static IdukayTestLoginResponse selectionRequired(List<SchoolOption> schools, List<ProfileOption> profiles) {

        return new IdukayTestLoginResponse(
                "SELECTION_REQUIRED", false, null, 0, List.copyOf(schools), List.copyOf(profiles));
    }
}
