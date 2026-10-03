package io.academicmonitor.communication.application;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationRepository;
import io.academicmonitor.communication.domain.CommunicationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommunicationWorkflowStore {
    private final CommunicationRepository repository;

    CommunicationWorkflowStore(CommunicationRepository repository) {
        this.repository = repository;
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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID communicationId, String code) {
        Communication communication = get(communicationId);
        communication.markFailed(code, "Provider delivery failed");
        repository.save(communication);
    }

    private Communication get(UUID communicationId) {
        return repository
                .findById(communicationId)
                .orElseThrow(
                        () -> new CommunicationWorkflowException(CommunicationWorkflowError.COMMUNICATION_NOT_FOUND));
    }
}
