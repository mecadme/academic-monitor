package io.academicmonitor.communication.application;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationRepository;
import io.academicmonitor.communication.domain.CommunicationStatus;
import io.academicmonitor.notification.application.AppNotificationService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommunicationWorkflowStore {
    private final CommunicationRepository repository;
    private final AppNotificationService notificationService;

    CommunicationWorkflowStore(CommunicationRepository repository, AppNotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Communication claimDraftForSending(UUID communicationId) {
        Communication communication = get(communicationId);
        if (communication.getStatus() != CommunicationStatus.DRAFT) {
            throw new CommunicationWorkflowException(CommunicationWorkflowError.COMMUNICATION_NOT_SENDABLE);
        }
        communication.beginSending();
        return repository.save(communication);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID communicationId) {
        Communication communication = get(communicationId);
        communication.markSent(Instant.now());
        repository.save(communication);
        notificationService.communicationSent(
                communication.getInstitutionId(), communication.getTeacherUserId(), communication.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID communicationId, String code) {
        Communication communication = get(communicationId);
        communication.markFailed(code, "Provider delivery failed");
        repository.save(communication);
        notificationService.communicationFailed(
                communication.getInstitutionId(), communication.getTeacherUserId(), communication.getId());
    }

    private Communication get(UUID communicationId) {
        return repository
                .findById(communicationId)
                .orElseThrow(
                        () -> new CommunicationWorkflowException(CommunicationWorkflowError.COMMUNICATION_NOT_FOUND));
    }
}
