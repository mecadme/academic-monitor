package io.academicmonitor.communication.infrastructure.persistence;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class CommunicationRepositoryAdapter implements CommunicationRepository {

    private final CommunicationDataRepository repository;

    CommunicationRepositoryAdapter(CommunicationDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Communication save(Communication communication) {
        return repository.saveAndFlush(communication);
    }

    @Override
    public Optional<Communication> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Communication> findByAlertId(UUID alertId) {
        return repository.findByAlertId(alertId);
    }

    @Override
    public List<Communication> findByAlertIdIn(java.util.Collection<UUID> alertIds) {
        return repository.findByAlertIdIn(alertIds);
    }

    @Override
    public List<Communication> findByInstitutionIdAndTeacherUserId(UUID institutionId, UUID teacherUserId) {
        return repository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId);
    }
}
