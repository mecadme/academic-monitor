package io.academicmonitor.communication.application;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.CourseEnrollment;
import io.academicmonitor.academic.domain.CourseEnrollmentRepository;
import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentGuardian;
import io.academicmonitor.academic.domain.StudentGuardianRepository;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.application.port.CommunicationDeliveryRequest;
import io.academicmonitor.communication.application.port.DeliveryResult;
import io.academicmonitor.communication.domain.Communication;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ManualNotificationService {

    private static final int SUBJECT_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 20_000;

    private final AcademicCourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;
    private final CommunicationAuditService auditService;
    private final CommunicationDeliveryPort deliveryPort;

    public ManualNotificationService(
            AcademicCourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            StudentGuardianRepository studentGuardianRepository,
            GuardianRepository guardianRepository,
            CommunicationAuditService auditService,
            CommunicationDeliveryPort deliveryPort) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.guardianRepository = guardianRepository;
        this.auditService = auditService;
        this.deliveryPort = deliveryPort;
    }

    public ManualNotificationResult send(ManualNotificationRequest request) {
        validateRequest(request);
        RecipientResolution resolution = resolveRecipient(request);
        if (resolution.failure() != null) return ManualNotificationResult.blocked(resolution.failure());

        Guardian guardian = resolution.guardian();
        Communication communication = auditService.createPending(
                request.institutionId(),
                request.teacherUserId(),
                request.studentId(),
                guardian.getId(),
                deliveryPort.providerCode(),
                request.subject().trim(),
                request.content().trim());

        DeliveryResult result;
        try {
            result = deliveryPort.send(new CommunicationDeliveryRequest(
                    communication.getId(),
                    request.institutionId(),
                    request.teacherUserId(),
                    guardian.getExternalUserId(),
                    request.subject(),
                    request.content()));
        } catch (RuntimeException exception) {
            auditService.markFailed(communication.getId(), "PROVIDER_UNAVAILABLE", "Provider delivery failed");
            return ManualNotificationResult.failed(
                    communication.getId(), ManualNotificationFailure.PROVIDER_UNAVAILABLE);
        }
        if (result == null) {
            auditService.markFailed(communication.getId(), "PROVIDER_UNAVAILABLE", "Provider delivery failed");
            return ManualNotificationResult.failed(
                    communication.getId(), ManualNotificationFailure.PROVIDER_UNAVAILABLE);
        }
        if (result.delivered()) {
            auditService.markSent(communication.getId());
            return ManualNotificationResult.sent(communication.getId());
        }

        ManualNotificationFailure failure = toFailure(result.failureCode());
        auditService.markFailed(communication.getId(), failure.name(), sanitizeFailureReason(result.failureReason()));
        return ManualNotificationResult.failed(communication.getId(), failure);
    }

    private RecipientResolution resolveRecipient(ManualNotificationRequest request) {
        Student student = studentRepository.findStudentById(request.studentId()).orElse(null);
        if (student == null
                || !request.institutionId().equals(student.getInstitutionId())
                || !isInTeacherScope(request)) {
            return RecipientResolution.failed(ManualNotificationFailure.STUDENT_OUT_OF_SCOPE);
        }

        List<StudentGuardian> officialGuardians =
                studentGuardianRepository.findByStudentId(request.studentId()).stream()
                        .filter(relationship -> request.institutionId().equals(relationship.getInstitutionId()))
                        .filter(StudentGuardian::isOfficialLegalGuardian)
                        .toList();
        if (officialGuardians.isEmpty())
            return RecipientResolution.failed(ManualNotificationFailure.RECIPIENT_NOT_FOUND);
        if (officialGuardians.size() > 1)
            return RecipientResolution.failed(ManualNotificationFailure.RECIPIENT_AMBIGUOUS);

        Guardian guardian = guardianRepository
                .findByInstitutionIdAndIdIn(
                        request.institutionId(),
                        Set.of(officialGuardians.getFirst().getGuardianId()))
                .stream()
                .findFirst()
                .orElse(null);
        if (guardian == null
                || guardian.getExternalUserId() == null
                || guardian.getExternalUserId().isBlank()
                || !guardian.hasSystemAccess()) {
            return RecipientResolution.failed(ManualNotificationFailure.RECIPIENT_NOT_REACHABLE);
        }
        return RecipientResolution.resolved(guardian);
    }

    private boolean isInTeacherScope(ManualNotificationRequest request) {
        Set<UUID> courseIds =
                courseRepository
                        .findByInstitutionIdAndTeacherUserId(request.institutionId(), request.teacherUserId())
                        .stream()
                        .map(AcademicCourse::getId)
                        .collect(Collectors.toUnmodifiableSet());
        return !courseIds.isEmpty()
                && enrollmentRepository.findEnrollmentsByCourseIdIn(courseIds).stream()
                        .map(CourseEnrollment::getStudentId)
                        .anyMatch(request.studentId()::equals);
    }

    private static void validateRequest(ManualNotificationRequest request) {
        if (request == null
                || request.institutionId() == null
                || request.teacherUserId() == null
                || request.studentId() == null) {
            throw new IllegalArgumentException("notification scope is required");
        }
        validateSubject(request.subject());
        validateContent(request.content());
    }

    private static void validateSubject(String subject) {
        if (subject == null || subject.isBlank() || subject.trim().length() > SUBJECT_MAX_LENGTH) {
            throw new IllegalArgumentException("subject must contain at most 200 characters");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank() || content.trim().length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("content must contain at most 20000 characters");
        }
        String normalized = content.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("<script")
                || normalized.contains("javascript:")
                || normalized.matches("(?s).*\\son[a-z]+\\s*=.*")) {
            throw new IllegalArgumentException("content contains unsupported HTML");
        }
    }

    private static ManualNotificationFailure toFailure(String failureCode) {
        if ("SENDER_NOT_RESOLVED".equals(failureCode)) return ManualNotificationFailure.SENDER_NOT_RESOLVED;
        if ("PROVIDER_REJECTED".equals(failureCode)) return ManualNotificationFailure.PROVIDER_REJECTED;
        return ManualNotificationFailure.PROVIDER_UNAVAILABLE;
    }

    private static String sanitizeFailureReason(String reason) {
        if (reason == null || reason.isBlank()) return "Provider delivery failed";
        String sanitized = reason.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() > 255 ? sanitized.substring(0, 255) : sanitized;
    }

    private record RecipientResolution(Guardian guardian, ManualNotificationFailure failure) {
        static RecipientResolution resolved(Guardian guardian) {
            return new RecipientResolution(guardian, null);
        }

        static RecipientResolution failed(ManualNotificationFailure failure) {
            return new RecipientResolution(null, failure);
        }
    }
}
