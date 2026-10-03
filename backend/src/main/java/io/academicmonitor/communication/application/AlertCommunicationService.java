package io.academicmonitor.communication.application;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.academic.domain.Grade;
import io.academicmonitor.academic.domain.GradeRepository;
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
import io.academicmonitor.communication.domain.CommunicationRepository;
import io.academicmonitor.communication.domain.CommunicationStatus;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertCommunicationService {
    private static final int SUBJECT_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 20_000;
    private static final ConcurrentMap<UUID, Object> DRAFT_LOCKS = new ConcurrentHashMap<>();

    private final AlertRepository alertRepository;
    private final AcademicCourseRepository courseRepository;
    private final ActivityRepository activityRepository;
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;
    private final CommunicationRepository communicationRepository;
    private final TemplateMessageGenerator messageGenerator;
    private final CommunicationDeliveryPort deliveryPort;
    private final CommunicationWorkflowStore workflowStore;

    public AlertCommunicationService(
            AlertRepository alertRepository,
            AcademicCourseRepository courseRepository,
            ActivityRepository activityRepository,
            GradeRepository gradeRepository,
            StudentRepository studentRepository,
            StudentGuardianRepository studentGuardianRepository,
            GuardianRepository guardianRepository,
            CommunicationRepository communicationRepository,
            TemplateMessageGenerator messageGenerator,
            CommunicationDeliveryPort deliveryPort,
            CommunicationWorkflowStore workflowStore) {
        this.alertRepository = alertRepository;
        this.courseRepository = courseRepository;
        this.activityRepository = activityRepository;
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.guardianRepository = guardianRepository;
        this.communicationRepository = communicationRepository;
        this.messageGenerator = messageGenerator;
        this.deliveryPort = deliveryPort;
        this.workflowStore = workflowStore;
    }

    @Transactional
    public CommunicationResponse prepare(UUID institutionId, UUID teacherUserId, UUID alertId) {
        Object lock = DRAFT_LOCKS.computeIfAbsent(alertId, ignored -> new Object());
        synchronized (lock) {
            return prepareLocked(institutionId, teacherUserId, alertId);
        }
    }

    private CommunicationResponse prepareLocked(UUID institutionId, UUID teacherUserId, UUID alertId) {
        Alert alert = ownedAlert(institutionId, teacherUserId, alertId);
        if (!alert.isOpen()) throw error(CommunicationWorkflowError.ALERT_NOT_ACTIVE);

        List<Communication> existing = communicationRepository.findByAlertId(alertId);
        Communication draft = existing.stream()
                .filter(communication -> communication.getStatus() == CommunicationStatus.DRAFT)
                .max(Comparator.comparing(
                        Communication::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        if (draft != null) return responseFor(draft);
        if (existing.stream().anyMatch(communication -> communication.getStatus() == CommunicationStatus.SENT)) {
            throw error(CommunicationWorkflowError.COMMUNICATION_ALREADY_SENT);
        }
        if (!existing.isEmpty()) throw error(CommunicationWorkflowError.COMMUNICATION_NOT_SENDABLE);

        Student student = studentRepository
                .findStudentById(alert.getStudentId())
                .filter(value -> institutionId.equals(value.getInstitutionId()))
                .orElseThrow(() -> error(CommunicationWorkflowError.STUDENT_OUT_OF_SCOPE));
        Guardian guardian = resolveOfficialGuardian(institutionId, student);
        AcademicCourse course = ownedCourse(institutionId, teacherUserId, alert.getCourseId());
        Activity activity = activityRepository.findActivitiesByCourseIdIn(Set.of(course.getId())).stream()
                .filter(value -> alert.getActivityId().equals(value.getId()))
                .findFirst()
                .orElseThrow(() -> error(CommunicationWorkflowError.ALERT_NOT_FOUND));
        Grade grade = gradeRepository
                .findByActivityIdAndStudentId(activity.getId(), student.getId())
                .orElse(null);
        TemplateMessageGenerator.GeneratedMessage message = messageGenerator.generate(
                course.getSubject() == null ? course.getName() : course.getSubject(),
                activity.getName(),
                grade == null ? alert.getScoreSnapshot() : grade.getScore(),
                activity.getMaxScore());
        Communication communication = Communication.draft(
                institutionId,
                teacherUserId,
                student.getId(),
                guardian.getId(),
                alert.getId(),
                deliveryPort.providerCode(),
                message.subject(),
                message.content());
        return responseFor(communicationRepository.save(communication));
    }

    @Transactional(readOnly = true)
    public CommunicationResponse get(UUID institutionId, UUID teacherUserId, UUID communicationId) {
        return responseFor(ownedCommunication(institutionId, teacherUserId, communicationId));
    }

    @Transactional(readOnly = true)
    public List<CommunicationResponse> list(UUID institutionId, UUID teacherUserId, CommunicationStatus status) {
        return communicationRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId).stream()
                .filter(communication -> status == null || status == communication.getStatus())
                .sorted(Comparator.comparing(
                                Communication::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Communication::getId))
                .map(this::responseFor)
                .toList();
    }

    @Transactional
    public CommunicationResponse edit(
            UUID institutionId, UUID teacherUserId, UUID communicationId, String subject, String content) {
        validateMessage(subject, content);
        Communication communication = ownedCommunication(institutionId, teacherUserId, communicationId);
        if (communication.getStatus() != CommunicationStatus.DRAFT) {
            throw error(CommunicationWorkflowError.COMMUNICATION_NOT_EDITABLE);
        }
        communication.editDraft(subject.trim(), content.trim());
        return responseFor(communicationRepository.save(communication));
    }

    public CommunicationResponse send(UUID institutionId, UUID teacherUserId, UUID communicationId) {
        Communication communication = ownedCommunication(institutionId, teacherUserId, communicationId);
        if (communication.getStatus() == CommunicationStatus.SENT)
            throw error(CommunicationWorkflowError.COMMUNICATION_ALREADY_SENT);
        if (communication.getStatus() != CommunicationStatus.DRAFT)
            throw error(CommunicationWorkflowError.COMMUNICATION_NOT_SENDABLE);
        Guardian guardian = validGuardian(institutionId, communication.getStudentId(), communication.getGuardianId());
        Communication claimed = workflowStore.claimDraftForSending(communicationId);
        try {
            DeliveryResult result = deliveryPort.send(new CommunicationDeliveryRequest(
                    claimed.getId(),
                    institutionId,
                    teacherUserId,
                    guardian.getExternalUserId(),
                    claimed.getSubject(),
                    claimed.getContent()));
            if (result != null && result.delivered()) {
                workflowStore.markSent(communicationId);
            } else {
                workflowStore.markFailed(communicationId, deliveryFailureCode(result));
            }
        } catch (RuntimeException exception) {
            workflowStore.markFailed(communicationId, "PROVIDER_UNAVAILABLE");
        }
        return get(institutionId, teacherUserId, communicationId);
    }

    private CommunicationResponse responseFor(Communication communication) {
        Student student =
                studentRepository.findStudentById(communication.getStudentId()).orElse(null);
        Alert alert = alertRepository.findById(communication.getAlertId()).orElse(null);
        AcademicCourse course = alert == null
                ? null
                : courseRepository
                        .findByInstitutionIdAndTeacherUserId(
                                communication.getInstitutionId(), communication.getTeacherUserId())
                        .stream()
                        .filter(value -> alert.getCourseId().equals(value.getId()))
                        .findFirst()
                        .orElse(null);
        Activity activity = course == null || alert == null
                ? null
                : activityRepository.findActivitiesByCourseIdIn(Set.of(course.getId())).stream()
                        .filter(value -> alert.getActivityId().equals(value.getId()))
                        .findFirst()
                        .orElse(null);
        return CommunicationResponse.from(
                communication,
                student == null ? null : student.getFullName(),
                course == null ? null : course.getName(),
                course == null ? null : course.getSubject(),
                activity == null ? null : activity.getName(),
                alert == null ? null : alert.getScoreSnapshot(),
                activity == null ? null : activity.getMaxScore(),
                alert == null ? null : alert.getSeverity());
    }

    private Alert ownedAlert(UUID institutionId, UUID teacherUserId, UUID alertId) {
        Alert alert =
                alertRepository.findById(alertId).orElseThrow(() -> error(CommunicationWorkflowError.ALERT_NOT_FOUND));
        if (!institutionId.equals(alert.getInstitutionId())) throw error(CommunicationWorkflowError.ALERT_NOT_FOUND);
        ownedCourse(institutionId, teacherUserId, alert.getCourseId());
        return alert;
    }

    private AcademicCourse ownedCourse(UUID institutionId, UUID teacherUserId, UUID courseId) {
        return courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId).stream()
                .filter(course -> courseId.equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> error(CommunicationWorkflowError.STUDENT_OUT_OF_SCOPE));
    }

    private Communication ownedCommunication(UUID institutionId, UUID teacherUserId, UUID communicationId) {
        return communicationRepository
                .findById(communicationId)
                .filter(value -> institutionId.equals(value.getInstitutionId()))
                .filter(value -> teacherUserId.equals(value.getTeacherUserId()))
                .orElseThrow(() -> error(CommunicationWorkflowError.COMMUNICATION_NOT_FOUND));
    }

    private Guardian resolveOfficialGuardian(UUID institutionId, Student student) {
        List<StudentGuardian> relationships = studentGuardianRepository.findByStudentId(student.getId()).stream()
                .filter(value -> institutionId.equals(value.getInstitutionId()))
                .filter(StudentGuardian::isOfficialLegalGuardian)
                .toList();
        if (relationships.isEmpty()) throw error(CommunicationWorkflowError.RECIPIENT_NOT_FOUND);
        if (relationships.size() > 1) throw error(CommunicationWorkflowError.RECIPIENT_AMBIGUOUS);
        return guardianRepository
                .findByInstitutionIdAndIdIn(
                        institutionId, Set.of(relationships.getFirst().getGuardianId()))
                .stream()
                .findFirst()
                .filter(AlertCommunicationService::isReachable)
                .orElseThrow(() -> error(CommunicationWorkflowError.RECIPIENT_NOT_REACHABLE));
    }

    private Guardian validGuardian(UUID institutionId, UUID studentId, UUID guardianId) {
        boolean official = studentGuardianRepository.findByStudentId(studentId).stream()
                .anyMatch(value -> institutionId.equals(value.getInstitutionId())
                        && guardianId.equals(value.getGuardianId())
                        && value.isOfficialLegalGuardian());
        if (!official) throw error(CommunicationWorkflowError.RECIPIENT_NOT_REACHABLE);
        return guardianRepository.findByInstitutionIdAndIdIn(institutionId, Set.of(guardianId)).stream()
                .findFirst()
                .filter(AlertCommunicationService::isReachable)
                .orElseThrow(() -> error(CommunicationWorkflowError.RECIPIENT_NOT_REACHABLE));
    }

    private static boolean isReachable(Guardian guardian) {
        return guardian.hasSystemAccess()
                && guardian.getExternalUserId() != null
                && !guardian.getExternalUserId().isBlank();
    }

    private static void validateMessage(String subject, String content) {
        if (subject == null || subject.isBlank() || subject.trim().length() > SUBJECT_MAX_LENGTH) {
            throw new IllegalArgumentException("subject must contain at most 200 characters");
        }
        if (content == null || content.isBlank() || content.trim().length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("content must contain at most 20000 characters");
        }
        String normalized = content.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("<script")
                || normalized.contains("javascript:")
                || normalized.contains("<iframe")
                || normalized.contains("<object")
                || normalized.contains("<embed")
                || normalized.contains("<link")
                || normalized.contains("<style")
                || normalized.contains("data:text/html")
                || normalized.matches("(?s).*\\s+on[a-z]+\\s*=.*")) {
            throw new IllegalArgumentException("content contains unsupported HTML");
        }
    }

    private static String deliveryFailureCode(DeliveryResult result) {
        if (result == null
                || result.failureCode() == null
                || result.failureCode().isBlank()) return "PROVIDER_UNAVAILABLE";
        return result.failureCode().trim().length() > 64
                ? "PROVIDER_REJECTED"
                : result.failureCode().trim();
    }

    private static CommunicationWorkflowException error(CommunicationWorkflowError error) {
        return new CommunicationWorkflowException(error);
    }
}
