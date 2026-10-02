package io.academicmonitor.integration.idukay.guardian;

import static org.junit.jupiter.api.Assertions.*;

import io.academicmonitor.academic.application.port.PlatformStudentGuardianSnapshot;
import io.academicmonitor.integration.idukay.course.IdukayParentReferenceDto;
import io.academicmonitor.integration.idukay.course.IdukayStudentNameDto;
import io.academicmonitor.integration.idukay.course.IdukayStudentRelativeDto;
import org.junit.jupiter.api.Test;

class IdukayGuardianMapperTest {

    @Test
    void mapsParentProfileAndUserIdentitiesSeparatelyWithRelationshipFacts() {
        IdukayParentDto parent = new IdukayParentDto(
                "guardian-profile-alpha",
                new IdukayParentUserDto(
                        "guardian-user-alpha", "Guardian", "One", "Example", null, "guardian.one@example.test", true),
                false,
                new IdukayParentRelationalDataDto(new IdukayStudentNameDto("Guardian One", null), null));
        IdukayStudentRelativeDto relative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);

        PlatformStudentGuardianSnapshot mapped = IdukayGuardianMapper.toSnapshot("student-alpha", relative, parent);

        assertEquals("student-alpha", mapped.studentExternalId());
        assertEquals("guardian-profile-alpha", mapped.guardianExternalId());
        assertEquals("guardian-user-alpha", mapped.guardianUserExternalId());
        assertEquals("Guardian One", mapped.displayName());
        assertEquals("guardian.one@example.test", mapped.email());
        assertFalse(mapped.systemAccess());
        assertEquals("Parent", mapped.relationship());
        assertTrue(mapped.officialLegalGuardian());
        assertTrue(mapped.legalGuardian());
        assertFalse(mapped.economicRepresentative());
        assertTrue(mapped.canPickUp());
        assertTrue(mapped.livesWithStudent());
    }

    @Test
    void normalizesBlankEmailAndDefaultsMissingOptionalFlagsToFalse() {
        IdukayParentDto parent = new IdukayParentDto(
                "guardian-profile-beta",
                new IdukayParentUserDto("guardian-user-beta", "Guardian", null, "Two", null, "  ", true),
                null,
                null);
        IdukayStudentRelativeDto relative = new IdukayStudentRelativeDto(
                "relation-beta",
                new IdukayParentReferenceDto("guardian-profile-beta"),
                null,
                null,
                null,
                null,
                null,
                null);

        PlatformStudentGuardianSnapshot mapped = IdukayGuardianMapper.toSnapshot("student-beta", relative, parent);

        assertEquals("Guardian Two", mapped.displayName());
        assertNull(mapped.email());
        assertTrue(mapped.systemAccess());
        assertFalse(mapped.officialLegalGuardian());
        assertFalse(mapped.legalGuardian());
        assertFalse(mapped.economicRepresentative());
        assertFalse(mapped.canPickUp());
        assertFalse(mapped.livesWithStudent());
    }

    @Test
    void skipsParentWithoutExternalUserIdentity() {
        IdukayParentDto parent = new IdukayParentDto(
                "guardian-profile-gamma",
                new IdukayParentUserDto(null, "Guardian", null, "Three", null, null, null),
                null,
                null);

        assertNull(IdukayGuardianMapper.toSnapshot(
                "student-gamma",
                new IdukayStudentRelativeDto(
                        "relation-gamma",
                        new IdukayParentReferenceDto("guardian-profile-gamma"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                parent));
    }
}
