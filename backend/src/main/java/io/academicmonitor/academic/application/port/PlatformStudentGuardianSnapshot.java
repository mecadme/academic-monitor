package io.academicmonitor.academic.application.port;

public record PlatformStudentGuardianSnapshot(
        String studentExternalId,
        String guardianExternalId,
        String guardianUserExternalId,
        String displayName,
        String email,
        boolean systemAccess,
        String relationship,
        boolean officialLegalGuardian,
        boolean legalGuardian,
        boolean economicRepresentative,
        boolean canPickUp,
        boolean livesWithStudent) {}
