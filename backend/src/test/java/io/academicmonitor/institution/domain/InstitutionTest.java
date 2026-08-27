package io.academicmonitor.institution.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InstitutionTest {

    @Test
    void shouldCreateActiveInstitution() {
        Institution institution = new Institution("La Providencia", "America/Guayaquil");

        assertEquals("La Providencia", institution.getName());
        assertEquals("America/Guayaquil", institution.getTimezone());
        assertTrue(institution.isActive());
    }

    @Test
    void shouldTrimInstitutionName() {
        Institution institution = new Institution("  La Providencia  ", "America/Guayaquil");

        assertEquals("La Providencia", institution.getName());
    }

    @Test
    void shouldRejectBlankInstitutionName() {
        assertThrows(IllegalArgumentException.class, () -> new Institution("   ", "America/Guayaquil"));
    }

    @Test
    void shouldRenameInstitution() {
        Institution institution = new Institution("Nombre inicial", "America/Guayaquil");

        institution.rename("Nuevo nombre");

        assertEquals("Nuevo nombre", institution.getName());
    }

    @Test
    void shouldDeactivateAndActivateInstitution() {
        Institution institution = new Institution("La Providencia", "America/Guayaquil");

        institution.deactivate();

        assertFalse(institution.isActive());

        institution.activate();

        assertTrue(institution.isActive());
    }

    @Test
    void shouldRejectBlankTimezone() {
        assertThrows(IllegalArgumentException.class, () -> new Institution("La Providencia", "   "));
    }
}
