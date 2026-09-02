package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.Grade;
import io.academicmonitor.academic.domain.GradeRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AcademicRepositoryAdapter
        implements AcademicCourseRepository,
                StudentRepository,
                CourseEnrollmentRepository,
                ActivityRepository,
                GradeRepository {

    private final AcademicCourseDataRepository courseRepository;
    private final StudentDataRepository studentRepository;
    private final CourseEnrollmentDataRepository enrollmentRepository;
    private final ActivityDataRepository activityRepository;
    private final GradeDataRepository gradeRepository;

    AcademicRepositoryAdapter(
            AcademicCourseDataRepository courseRepository,
            StudentDataRepository studentRepository,
            CourseEnrollmentDataRepository enrollmentRepository,
            ActivityDataRepository activityRepository,
            GradeDataRepository gradeRepository) {

        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
    }

    @Override
    public AcademicCourse save(AcademicCourse course) {
        return courseRepository.save(course);
    }

    @Override
    public Student save(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public CourseEnrollment save(CourseEnrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public Activity save(Activity activity) {
        return activityRepository.save(activity);
    }

    @Override
    public Grade save(Grade grade) {
        return gradeRepository.save(grade);
    }

    @Override
    public Optional<AcademicCourse> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId) {

        return courseRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                institutionId, platformCode, externalId);
    }

    @Override
    public List<AcademicCourse> findByTeacherUserId(UUID teacherUserId) {
        return courseRepository.findByTeacherUserId(teacherUserId);
    }

    @Override
    public List<AcademicCourse> findByInstitutionIdAndTeacherUserId(UUID institutionId, UUID teacherUserId) {
        return courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId);
    }

    @Override
    public Optional<Student> findStudentByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId) {

        return studentRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                institutionId, platformCode, externalId);
    }

    @Override
    public boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId) {

        return enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId);
    }

    @Override
    public Optional<Activity> findByCourseIdAndPlatformCodeAndExternalId(
            UUID courseId, String platformCode, String externalId) {

        return activityRepository.findByCourseIdAndPlatformCodeAndExternalId(courseId, platformCode, externalId);
    }

    @Override
    public Optional<Grade> findByActivityIdAndStudentId(UUID activityId, UUID studentId) {

        return gradeRepository.findByActivityIdAndStudentId(activityId, studentId);
    }

    @Override
    public List<CourseEnrollment> findByCourseId(UUID courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Override
    public List<CourseEnrollment> findEnrollmentsByCourseIdIn(Collection<UUID> courseIds) {
        return enrollmentRepository.findByCourseIdIn(courseIds);
    }

    @Override
    public Optional<Student> findStudentById(UUID studentId) {
        return studentRepository.findById(studentId);
    }

    @Override
    public List<Student> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> studentIds) {
        return studentRepository.findByInstitutionIdAndIdIn(institutionId, studentIds);
    }

    @Override
    public Optional<Activity> findLatestByCourseId(UUID courseId) {
        return activityRepository.findTopByCourseIdOrderByDueDateDesc(courseId);
    }

    @Override
    public List<Activity> findActivitiesByCourseIdIn(Collection<UUID> courseIds) {
        return activityRepository.findByCourseIdIn(courseIds);
    }
}
