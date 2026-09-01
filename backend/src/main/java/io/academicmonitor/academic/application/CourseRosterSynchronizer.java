package io.academicmonitor.academic.application;

import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentSnapshot;
import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CourseRosterSynchronizer {

    private final AcademicCourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public CourseRosterSynchronizer(
            AcademicCourseRepository courseRepository,
            StudentRepository studentRepository,
            CourseEnrollmentRepository enrollmentRepository) {

        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    AcademicCourse synchronize(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            PlatformCourseSnapshot platformCourse,
            UUID academicYearId) {

        AcademicCourse course =
                synchronizeCourse(institutionId, teacherUserId, platformCode, platformCourse, academicYearId);

        synchronizeStudents(institutionId, platformCode, course, platformCourse);

        return course;
    }

    private AcademicCourse synchronizeCourse(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            PlatformCourseSnapshot platformCourse,
            UUID academicYearId) {

        return courseRepository
                .findByInstitutionIdAndPlatformCodeAndExternalId(
                        institutionId, platformCode, platformCourse.externalId())
                .map(existing -> associateAcademicYear(existing, academicYearId))
                .orElseGet(() -> {
                    AcademicCourse created = new AcademicCourse(
                            institutionId,
                            teacherUserId,
                            academicYearId,
                            platformCode,
                            platformCourse.externalId(),
                            platformCourse.name(),
                            platformCourse.subject());

                    created.enableMonitoring();

                    return courseRepository.save(created);
                });
    }

    private AcademicCourse associateAcademicYear(AcademicCourse course, UUID academicYearId) {
        if (course.associateAcademicYear(academicYearId)) {
            return courseRepository.save(course);
        }
        return course;
    }

    private void synchronizeStudents(
            UUID institutionId, String platformCode, AcademicCourse course, PlatformCourseSnapshot platformCourse) {

        for (PlatformStudentSnapshot platformStudent : platformCourse.students()) {

            Student student = studentRepository
                    .findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                            institutionId, platformCode, platformStudent.externalId())
                    .orElseGet(() -> studentRepository.save(new Student(
                            institutionId,
                            platformCode,
                            platformStudent.externalId(),
                            platformStudent.firstName(),
                            platformStudent.lastName())));

            if (!enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), student.getId())) {

                enrollmentRepository.save(new CourseEnrollment(course.getId(), student.getId()));
            }
        }
    }
}
