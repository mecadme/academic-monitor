package io.academicmonitor.communication.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationRepository {
    Communication save(Communication communication);

    Optional<Communication> findById(UUID id);

    List<Communication> findByAlertId(UUID alertId);
}
