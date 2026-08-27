package io.academicmonitor.institution.infrastructure.persistence;

import io.academicmonitor.institution.domain.Institution;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionDataRepository extends JpaRepository<Institution, UUID> {}
