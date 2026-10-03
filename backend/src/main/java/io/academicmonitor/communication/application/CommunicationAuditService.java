package io.academicmonitor.communication.application;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationRepository;
import io.academicmonitor.notification.application.AppNotificationService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunicationAuditService {

    private final CommunicationRepository repository;
    private final AppNotificationService notificationService;

    public CommunicationAuditService(CommunicationRepository repository, AppNotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Communication createPending(
            UUID institutionId,
            UUID teacherUserId,
            UUID studentId,
            UUID guardianId,
            String providerCode,
            String subject,
            String content) {
        return repository.save(new Communication(
                institutionId,
                teacherUserId,
                studentId,
                guardianId,
                "PLATFORM_NOTIFICATION",
                providerCode,
                subject,
                content));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID communicationId) {
        Communication communication = getCommunication(communicationId);
        communication.markSent(Instant.now());
        repository.save(communication);
        notificationService.communicationSent(
                communication.getInstitutionId(), communication.getTeacherUserId(), communication.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID communicationId, String failureCode, String failureReason) {
        Communication communication = getCommunication(communicationId);
        communication.markFailed(failureCode, failureReason);
        repository.save(communication);
        notificationService.communicationFailed(
                communication.getInstitutionId(), communication.getTeacherUserId(), communication.getId());
    }

    private Communication getCommunication(UUID communicationId) {
        return repository
                .findById(communicationId)
                .orElseThrow(() -> new IllegalStateException("Communication audit record was not found"));
    }
}
