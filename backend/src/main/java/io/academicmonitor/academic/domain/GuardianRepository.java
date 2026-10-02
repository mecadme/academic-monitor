package io.academicmonitor.academic.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository {
    Guardian save(Guardian guardian);

    Optional<Guardian> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId);

    List<Guardian> findByInstitutionIdAndPlatformCodeAndExternalIdIn(
            UUID institutionId, String platformCode, Collection<String> externalIds);

    List<Guardian> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> guardianIds);
}
