package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.StudentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseRosterSynchronizerTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACADEMIC_YEAR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_ACADEMIC_YEAR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String PLATFORM = "TEST";

    @Test
    void enrichesLegacyCourseWithoutAcademicYear() {
        AcademicCourseRepository courseRepository = mock(AcademicCourseRepository.class);
        AcademicCourse legacyCourse = mock(AcademicCourse.class);

        when(courseRepository.findByInstitutionIdAndPlatformCodeAndExternalId(INSTITUTION_ID, PLATFORM, "course-001"))
                .thenReturn(Optional.of(legacyCourse));
        when(legacyCourse.associateAcademicYear(ACADEMIC_YEAR_ID)).thenReturn(true);
        when(courseRepository.save(legacyCourse)).thenReturn(legacyCourse);

        CourseRosterSynchronizer synchronizer = synchronizer(courseRepository);

        AcademicCourse result =
                synchronizer.synchronize(INSTITUTION_ID, TEACHER_ID, PLATFORM, platformCourse(), ACADEMIC_YEAR_ID);

        assertSame(legacyCourse, result);
        verify(legacyCourse).associateAcademicYear(ACADEMIC_YEAR_ID);
        verify(courseRepository).save(legacyCourse);
    }

    @Test
    void refusesToReassignCourseToDifferentAcademicYear() {
        AcademicCourseRepository courseRepository = mock(AcademicCourseRepository.class);
        AcademicCourse course = new AcademicCourse(
                INSTITUTION_ID, TEACHER_ID, ACADEMIC_YEAR_ID, PLATFORM, "course-001", "Course", "Physics");

        when(courseRepository.findByInstitutionIdAndPlatformCodeAndExternalId(INSTITUTION_ID, PLATFORM, "course-001"))
                .thenReturn(Optional.of(course));

        CourseRosterSynchronizer synchronizer = synchronizer(courseRepository);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> synchronizer.synchronize(
                        INSTITUTION_ID, TEACHER_ID, PLATFORM, platformCourse(), OTHER_ACADEMIC_YEAR_ID));

        assertEquals("Course course-001 is already associated with a different academic year", exception.getMessage());
        verify(courseRepository, never()).save(course);
    }

    private static CourseRosterSynchronizer synchronizer(AcademicCourseRepository courseRepository) {
        return new CourseRosterSynchronizer(
                courseRepository, mock(StudentRepository.class), mock(CourseEnrollmentRepository.class));
    }

    private static PlatformCourseSnapshot platformCourse() {
        return new PlatformCourseSnapshot("course-001", "Course", "Physics", null, List.of(), List.of());
    }
}
