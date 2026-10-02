package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.academicmonitor.academic.domain.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudentGuardianQueryServiceTest {
    @Test
    void returnsNoGuardiansWhenStudentIsOutsideTeacherCourseScope() {
        UUID institutionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID teacherId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID studentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        AcademicCourseRepository courses = mock(AcademicCourseRepository.class);
        CourseEnrollmentRepository enrollments = mock(CourseEnrollmentRepository.class);
        StudentGuardianRepository relationships = mock(StudentGuardianRepository.class);
        GuardianRepository guardians = mock(GuardianRepository.class);
        when(courses.findByInstitutionIdAndTeacherUserId(institutionId, teacherId))
                .thenReturn(List.of());

        var service = new StudentGuardianQueryService(courses, enrollments, relationships, guardians);

        assertTrue(service.getGuardians(studentId, institutionId, teacherId).isEmpty());
        verifyNoInteractions(enrollments, relationships, guardians);
    }
}
