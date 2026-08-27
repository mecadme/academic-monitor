package io.academicmonitor.institution.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstitutionMembershipTest {

    @Test
    void shouldCreateMembership() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        InstitutionMembership membership = new InstitutionMembership(userId, institutionId, InstitutionRole.TEACHER);

        assertEquals(userId, membership.getUserId());
        assertEquals(institutionId, membership.getInstitutionId());
        assertEquals(InstitutionRole.TEACHER, membership.getInstitutionRole());
        assertTrue(membership.isActive());
    }

    @Test
    void shouldChangeInstitutionRole() {
        InstitutionMembership membership =
                new InstitutionMembership(UUID.randomUUID(), UUID.randomUUID(), InstitutionRole.TEACHER);

        membership.changeRole(InstitutionRole.ADMIN);

        assertEquals(InstitutionRole.ADMIN, membership.getInstitutionRole());
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InstitutionMembership(null, UUID.randomUUID(), InstitutionRole.TEACHER));
    }

    @Test
    void shouldRejectNullInstitutionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InstitutionMembership(UUID.randomUUID(), null, InstitutionRole.TEACHER));
    }

    @Test
    void shouldRejectNullRole() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InstitutionMembership(UUID.randomUUID(), UUID.randomUUID(), null));
    }

    @Test
    void shouldDeactivateMembership() {
        InstitutionMembership membership =
                new InstitutionMembership(UUID.randomUUID(), UUID.randomUUID(), InstitutionRole.TEACHER);

        membership.deactivate();

        assertFalse(membership.isActive());
    }
}
