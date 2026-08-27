package io.academicmonitor.institution.infrastructure.persistence;

import io.academicmonitor.institution.domain.InstitutionMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InstitutionMembershipDataRepository extends JpaRepository<InstitutionMembership, UUID> {

    Optional<InstitutionMembership> findByUserIdAndInstitutionId(UUID userId, UUID institutionId);

    List<InstitutionMembership> findByUserId(UUID userId);

    List<InstitutionMembership> findByInstitutionId(UUID institutionId);
}
