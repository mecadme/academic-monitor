package io.academicmonitor.integration.idukay.course;

import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import java.util.List;

public final class IdukayCourseMapper {

    private IdukayCourseMapper() {}

    public static PlatformCourseSnapshot toSnapshot(IdukayTeacherCourseDto course) {

        if (course == null) {
            throw new IllegalArgumentException("course is required");
        }

        String externalId = requireText(course.id(), "course._id");

        String name = requireText(course.name(), "course.name");

        if (course.subject() == null) {
            throw new IdukayApiException("Idukay course did not contain a subject");
        }

        String subject = requireText(course.subject().name(), "course.subject.name");

        return new PlatformCourseSnapshot(externalId, name, subject, List.of(), List.of());
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IdukayApiException("Idukay response did not contain " + field);
        }

        return value.trim();
    }
}
