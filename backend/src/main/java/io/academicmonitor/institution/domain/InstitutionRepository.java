package io.academicmonitor.institution.domain;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository {

    Institution save(Institution institution);

    Optional<Institution> findById(UUID id);
}
