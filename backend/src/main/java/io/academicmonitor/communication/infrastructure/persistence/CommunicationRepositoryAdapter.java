package io.academicmonitor.communication.infrastructure.persistence;

import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationRepository;
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
}
