package io.academicmonitor.institution.infrastructure.persistence;

import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionRepositoryAdapter implements InstitutionRepository {

    private final InstitutionDataRepository repository;

    public InstitutionRepositoryAdapter(InstitutionDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Institution save(Institution institution) {
        return repository.save(institution);
    }

    @Override
    public Optional<Institution> findById(UUID id) {
        return repository.findById(id);
    }
}
