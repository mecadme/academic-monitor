package io.academicmonitor.institution.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InstitutionRepositoryAdapterIT extends PostgresIntegrationTest {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void shouldPersistAndFindInstitutionById() {
        Institution institution = new Institution("La Providencia", "America/Guayaquil");

        Institution saved = institutionRepository.save(institution);

        assertNotNull(saved.getId());
        assertEquals(7, saved.getId().version());

        Optional<Institution> found = institutionRepository.findById(saved.getId());

        assertTrue(found.isPresent());

        Institution persisted = found.orElseThrow();

        assertEquals("La Providencia", persisted.getName());
        assertEquals("America/Guayaquil", persisted.getTimezone());
        assertTrue(persisted.isActive());
    }
}
