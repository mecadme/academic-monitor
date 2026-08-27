package io.academicmonitor.institution.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionMembershipRepository {

    InstitutionMembership save(InstitutionMembership membership);

    Optional<InstitutionMembership> findByUserIdAndInstitutionId(UUID userId, UUID institutionId);

    List<InstitutionMembership> findByUserId(UUID userId);

    List<InstitutionMembership> findByInstitutionId(UUID institutionId);
}
