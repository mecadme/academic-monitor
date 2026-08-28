package io.academicmonitor.academic.application.port;

import java.util.List;

public record PlatformCourseSnapshot(
        String externalId,
        String name,
        String subject,
        List<PlatformActivitySnapshot> activities,
        List<PlatformStudentSnapshot> students) {

    public PlatformCourseSnapshot {
        activities = activities == null ? List.of() : List.copyOf(activities);

        students = students == null ? List.of() : List.copyOf(students);
    }
}
