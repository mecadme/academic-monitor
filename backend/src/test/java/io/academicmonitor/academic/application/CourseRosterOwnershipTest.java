package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseRosterOwnershipTest {
    @Test
    void providerCourseAlreadyOwnedByAnotherTeacherCannotBeSynchronizedOrMutated() {
        UUID institution = UUID.randomUUID();
        UUID teacherA = UUID.randomUUID();
        UUID teacherB = UUID.randomUUID();
        UUID year = UUID.randomUUID();
        var courses = mock(AcademicCourseRepository.class);
        var students = mock(StudentRepository.class);
        var enrollments = mock(CourseEnrollmentRepository.class);
        var existing = mock(AcademicCourse.class);
        when(existing.getTeacherUserId()).thenReturn(teacherA);
        when(courses.findByInstitutionIdAndPlatformCodeAndExternalId(institution, "IDUKAY", "same-course"))
                .thenReturn(Optional.of(existing));
        var snapshot = mock(PlatformCourseSnapshot.class);
        when(snapshot.externalId()).thenReturn("same-course");
        var synchronizer = new CourseRosterSynchronizer(courses, students, enrollments);

        assertThrows(
                AcademicCourseOwnershipException.class,
                () -> synchronizer.synchronize(institution, teacherB, "IDUKAY", snapshot, year));

        verify(existing, never()).associateAcademicYear(any());
        verify(existing, never()).updateMetadata(any(), any());
        verify(courses, never()).save(any());
        verifyNoInteractions(students, enrollments);
    }
}
