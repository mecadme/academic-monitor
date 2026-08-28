package io.academicmonitor.academic.application.port;

import java.util.UUID;

public record AcademicPlatformContext(UUID institutionId, UUID teacherUserId) {

    public AcademicPlatformContext {
        if (institutionId == null) {
            throw new IllegalArgumentException("institutionId is required");
        }

        if (teacherUserId == null) {
            throw new IllegalArgumentException("teacherUserId is required");
        }
    }
}
