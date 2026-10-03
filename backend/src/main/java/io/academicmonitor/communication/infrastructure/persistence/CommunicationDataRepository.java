package io.academicmonitor.communication.infrastructure.persistence;

import io.academicmonitor.communication.domain.Communication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommunicationDataRepository extends JpaRepository<Communication, UUID> {
    List<Communication> findByAlertId(UUID alertId);
}
