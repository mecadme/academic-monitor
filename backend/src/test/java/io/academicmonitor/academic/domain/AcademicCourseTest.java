package io.academicmonitor.academic.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcademicCourseTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void updatesAndTrimsChangedMetadata() {
        AcademicCourse course = course("Old name", "Old subject");

        boolean changed = course.updateMetadata(" Updated name ", " Updated subject ");

        assertTrue(changed);
        assertEquals("Updated name", course.getName());
        assertEquals("Updated subject", course.getSubject());
    }

    @Test
    void reportsUnchangedMetadataAfterNormalization() {
        AcademicCourse course = course("Course", "Physics");

        boolean changed = course.updateMetadata(" Course ", " Physics ");

        assertFalse(changed);
        assertEquals("Course", course.getName());
        assertEquals("Physics", course.getSubject());
    }

    @Test
    void allowsSubjectToBecomeNull() {
        AcademicCourse course = course("Course", "Physics");

        boolean changed = course.updateMetadata("Course", null);

        assertTrue(changed);
        assertNull(course.getSubject());
    }

    @Test
    void rejectsBlankNameWithoutChangingMetadata() {
        AcademicCourse course = course("Course", "Physics");

        assertThrows(IllegalArgumentException.class, () -> course.updateMetadata(" ", "Chemistry"));

        assertEquals("Course", course.getName());
        assertEquals("Physics", course.getSubject());
    }

    private static AcademicCourse course(String name, String subject) {
        return new AcademicCourse(INSTITUTION_ID, TEACHER_ID, ACADEMIC_YEAR_ID, "IDUKAY", "course-001", name, subject);
    }
}
