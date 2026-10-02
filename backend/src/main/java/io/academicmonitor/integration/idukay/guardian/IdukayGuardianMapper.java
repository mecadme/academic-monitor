package io.academicmonitor.integration.idukay.guardian;

import io.academicmonitor.academic.application.port.PlatformStudentGuardianSnapshot;
import io.academicmonitor.integration.idukay.course.IdukayStudentRelativeDto;
import java.util.ArrayList;
import java.util.List;

public final class IdukayGuardianMapper {
    private IdukayGuardianMapper() {}

    public static PlatformStudentGuardianSnapshot toSnapshot(
            String studentExternalId, IdukayStudentRelativeDto relative, IdukayParentDto parent) {
        if (relative == null
                || parent == null
                || parent.user() == null
                || isBlank(parent.id())
                || isBlank(parent.user().id())) return null;
        String displayName = displayName(parent);
        if (displayName == null) return null;
        return new PlatformStudentGuardianSnapshot(
                studentExternalId,
                parent.id().trim(),
                parent.user().id().trim(),
                displayName,
                email(parent),
                systemAccess(parent),
                optionalText(relative.relationship()),
                Boolean.TRUE.equals(relative.officialLegalGuardian()),
                Boolean.TRUE.equals(relative.legalGuardian()),
                Boolean.TRUE.equals(relative.economicRepresentative()),
                Boolean.TRUE.equals(relative.canPickUp()),
                Boolean.TRUE.equals(relative.livesWith()));
    }

    private static String displayName(IdukayParentDto parent) {
        if (parent.relationalData() != null && parent.relationalData().name() != null) {
            String displayName = optionalText(parent.relationalData().name().show());
            if (displayName != null) return displayName;
        }
        List<String> parts = new ArrayList<>();
        add(parts, parent.user().name());
        add(parts, parent.user().secondName());
        add(parts, parent.user().surname());
        add(parts, parent.user().secondSurname());
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    // Parent-level access is authoritative; the user-level flag is its deterministic fallback.
    private static boolean systemAccess(IdukayParentDto parent) {
        if (parent.systemAccess() != null) return parent.systemAccess();
        return Boolean.TRUE.equals(parent.user().systemAccess());
    }

    private static String email(IdukayParentDto parent) {
        String userEmail = optionalText(parent.user().email());
        if (userEmail != null) return userEmail;
        return parent.relationalData() == null
                ? null
                : optionalText(parent.relationalData().email());
    }

    private static void add(List<String> parts, String value) {
        String normalized = optionalText(value);
        if (normalized != null) parts.add(normalized);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String optionalText(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
