package io.academicmonitor.institution.infrastructure.persistence;

import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionMembershipRepositoryAdapter implements InstitutionMembershipRepository {

    private final InstitutionMembershipDataRepository repository;

    public InstitutionMembershipRepositoryAdapter(InstitutionMembershipDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public InstitutionMembership save(InstitutionMembership membership) {
        return repository.save(membership);
    }

    @Override
    public Optional<InstitutionMembership> findByUserIdAndInstitutionId(UUID userId, UUID institutionId) {
        return repository.findByUserIdAndInstitutionId(userId, institutionId);
    }

    @Override
    public List<InstitutionMembership> findByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<InstitutionMembership> findByInstitutionId(UUID institutionId) {
        return repository.findByInstitutionId(institutionId);
    }
}
