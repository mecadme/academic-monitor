package io.academicmonitor.academic.application;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.academic.domain.StudentGuardian;
import io.academicmonitor.academic.domain.StudentGuardianRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentGuardianQueryService {
    private final AcademicCourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;

    public StudentGuardianQueryService(
            AcademicCourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            StudentGuardianRepository studentGuardianRepository,
            GuardianRepository guardianRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.guardianRepository = guardianRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentGuardianResponse> getGuardians(UUID studentId, UUID institutionId, UUID teacherUserId) {
        Objects.requireNonNull(studentId, "studentId is required");
        Objects.requireNonNull(institutionId, "institutionId is required");
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");
        Set<UUID> courseIds =
                courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId).stream()
                        .map(AcademicCourse::getId)
                        .collect(Collectors.toUnmodifiableSet());
        if (courseIds.isEmpty()) return List.of();
        boolean studentIsInTeacherScope = enrollmentRepository.findEnrollmentsByCourseIdIn(courseIds).stream()
                .map(CourseEnrollment::getStudentId)
                .anyMatch(studentId::equals);
        if (!studentIsInTeacherScope) return List.of();
        List<StudentGuardian> relationships = studentGuardianRepository.findByStudentId(studentId);
        if (relationships.isEmpty()) return List.of();
        Map<UUID, Guardian> guardiansById = guardianRepository
                .findByInstitutionIdAndIdIn(
                        institutionId,
                        relationships.stream()
                                .map(StudentGuardian::getGuardianId)
                                .collect(Collectors.toUnmodifiableSet()))
                .stream()
                .collect(Collectors.toUnmodifiableMap(Guardian::getId, Function.identity()));
        return relationships.stream()
                .filter(relationship -> institutionId.equals(relationship.getInstitutionId()))
                .filter(relationship -> guardiansById.containsKey(relationship.getGuardianId()))
                .sorted(Comparator.comparing(StudentGuardian::getId))
                .map(relationship -> toResponse(relationship, guardiansById.get(relationship.getGuardianId())))
                .toList();
    }

    private static StudentGuardianResponse toResponse(StudentGuardian relationship, Guardian guardian) {
        return new StudentGuardianResponse(
                guardian.getId(),
                guardian.getDisplayName(),
                guardian.getEmail(),
                guardian.hasSystemAccess(),
                relationship.getRelationship(),
                relationship.isOfficialLegalGuardian(),
                relationship.isLegalGuardian(),
                relationship.isEconomicRepresentative(),
                relationship.canPickUp(),
                relationship.livesWithStudent());
    }

    public record StudentGuardianResponse(
            UUID id,
            String displayName,
            String email,
            boolean systemAccess,
            String relationship,
            boolean officialLegalGuardian,
            boolean legalGuardian,
            boolean economicRepresentative,
            boolean canPickUp,
            boolean livesWithStudent) {}
}
