package io.academicmonitor.integration.idukay.course;

import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentSnapshot;
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

        List<PlatformStudentSnapshot> students = course.students().stream()
                .map(IdukayCourseMapper::toStudentSnapshot)
                .toList();

        return new PlatformCourseSnapshot(externalId, name, subject, null, List.of(), students);
    }

    private static PlatformStudentSnapshot toStudentSnapshot(IdukayStudentDto student) {

        if (student == null) {
            throw new IdukayApiException("Idukay course contained an empty student");
        }

        String externalId = requireText(student.id(), "student._id");

        if (student.relationalData() == null || student.relationalData().name() == null) {

            throw new IdukayApiException("Idukay student did not contain relational_data.name");
        }

        String displayName = requireText(student.relationalData().name().show(), "student.relational_data.name.show");

        ParsedName parsedName = parseDisplayName(displayName);

        return new PlatformStudentSnapshot(externalId, parsedName.firstName(), parsedName.lastName());
    }

    private static ParsedName parseDisplayName(String displayName) {

        int separator = displayName.indexOf(',');

        /*
         * Idukay commonly presents names as:
         *
         * SURNAME SECOND_SURNAME, FIRST SECOND
         */
        if (separator >= 0) {

            String lastName = displayName.substring(0, separator).trim();

            String firstName = displayName.substring(separator + 1).trim();

            return new ParsedName(
                    requireText(firstName, "student first name"), requireText(lastName, "student last name"));
        }

        /*
         * Defensive fallback when Idukay does not provide
         * the expected comma-separated display format.
         *
         * We preserve the complete display name instead of
         * guessing which tokens are surnames.
         */
        return new ParsedName(displayName, "");
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IdukayApiException("Idukay response did not contain " + field);
        }

        return value.trim();
    }

    private record ParsedName(String firstName, String lastName) {}
}
