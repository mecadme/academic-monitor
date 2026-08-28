package io.academicmonitor.academic.application.port;

import java.util.List;

public record AcademicPlatformSnapshot(List<PlatformCourseSnapshot> courses) {

    public AcademicPlatformSnapshot {
        courses = courses == null ? List.of() : List.copyOf(courses);
    }
}
