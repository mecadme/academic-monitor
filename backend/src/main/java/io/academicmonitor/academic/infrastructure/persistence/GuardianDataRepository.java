package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.Guardian;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GuardianDataRepository extends JpaRepository<Guardian, UUID> {
    Optional<Guardian> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);

    List<Guardian> findByInstitutionIdAndPlatformCodeAndExternalIdIn(
            UUID institutionId, String platformCode, Collection<String> externalIds);

    List<Guardian> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> guardianIds);
}
