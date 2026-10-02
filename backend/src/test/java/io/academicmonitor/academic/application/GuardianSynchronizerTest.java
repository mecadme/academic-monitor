package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.*;

import io.academicmonitor.academic.application.port.PlatformGuardianSyncSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentGuardianSnapshot;
import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentGuardian;
import io.academicmonitor.academic.domain.StudentGuardianRepository;
import io.academicmonitor.academic.domain.StudentRepository;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.Test;

class GuardianSynchronizerTest {
    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PLATFORM = "TEST";

    @Test
    void createsOneGuardianAndTwoRelationshipsForSiblingsSharingAParent() {
        Store store = new Store();
        store.students.put("student-alpha", student("student-alpha", "Student", "Alpha", 1));
        store.students.put("student-beta", student("student-beta", "Student", "Beta", 2));
        GuardianSynchronizer synchronizer = new GuardianSynchronizer(store.studentRepository(), store, store);

        GuardianSynchronizer.Result result = synchronizer.synchronize(
                INSTITUTION_ID,
                PLATFORM,
                List.of("student-alpha", "student-beta"),
                new PlatformGuardianSyncSnapshot(
                        List.of(
                                relationship("student-alpha", "guardian-profile-alpha", "Guardian One", true),
                                relationship("student-beta", "guardian-profile-alpha", "Guardian One", true)),
                        0));

        assertEquals(1, result.guardiansUpserted());
        assertEquals(2, result.guardianRelationshipsUpserted());
        assertEquals(1, store.guardians.size());
        assertEquals(2, store.relationships.size());
    }

    @Test
    void supportsMultipleGuardiansAndRepeatedSynchronizationWithoutDuplicates() {
        Store store = new Store();
        store.students.put("student-alpha", student("student-alpha", "Student", "Alpha", 1));
        GuardianSynchronizer synchronizer = new GuardianSynchronizer(store.studentRepository(), store, store);
        PlatformGuardianSyncSnapshot snapshot = new PlatformGuardianSyncSnapshot(
                List.of(
                        relationship("student-alpha", "guardian-profile-alpha", "Guardian One", true),
                        relationship("student-alpha", "guardian-profile-beta", "Guardian Two", false)),
                0);

        synchronizer.synchronize(INSTITUTION_ID, PLATFORM, List.of("student-alpha"), snapshot);
        GuardianSynchronizer.Result repeated =
                synchronizer.synchronize(INSTITUTION_ID, PLATFORM, List.of("student-alpha"), snapshot);

        assertEquals(2, store.guardians.size());
        assertEquals(2, store.relationships.size());
        assertEquals(0, repeated.guardiansUpserted());
        assertEquals(0, repeated.guardianRelationshipsUpserted());
    }

    @Test
    void refreshesContactAndRelationshipFactsAndSkipsMissingRecipientIdentity() {
        Store store = new Store();
        store.students.put("student-alpha", student("student-alpha", "Student", "Alpha", 1));
        GuardianSynchronizer synchronizer = new GuardianSynchronizer(store.studentRepository(), store, store);
        synchronizer.synchronize(
                INSTITUTION_ID,
                PLATFORM,
                List.of("student-alpha"),
                new PlatformGuardianSyncSnapshot(
                        List.of(relationship("student-alpha", "guardian-profile-alpha", "Guardian One", true)), 0));

        PlatformStudentGuardianSnapshot changed = new PlatformStudentGuardianSnapshot(
                "student-alpha",
                "guardian-profile-alpha",
                "guardian-user-alpha",
                "Guardian One Updated",
                "  ",
                false,
                "Parent",
                false,
                false,
                true,
                false,
                false);
        PlatformStudentGuardianSnapshot incomplete = new PlatformStudentGuardianSnapshot(
                "student-alpha",
                "guardian-profile-gamma",
                "",
                "Guardian Three",
                null,
                false,
                null,
                false,
                false,
                false,
                false,
                false);
        GuardianSynchronizer.Result result = synchronizer.synchronize(
                INSTITUTION_ID,
                PLATFORM,
                List.of("student-alpha"),
                new PlatformGuardianSyncSnapshot(List.of(changed, incomplete), 0));

        Guardian guardian = store.guardians.get("guardian-profile-alpha");
        StudentGuardian association = store.relationships.values().iterator().next();
        assertEquals("Guardian One Updated", guardian.getDisplayName());
        assertNull(guardian.getEmail());
        assertFalse(guardian.hasSystemAccess());
        assertFalse(association.isOfficialLegalGuardian());
        assertTrue(association.isEconomicRepresentative());
        assertEquals(1, result.guardianWarnings());
        assertEquals(1, store.guardians.size());
    }

    private static PlatformStudentGuardianSnapshot relationship(
            String studentId, String guardianId, String name, boolean official) {
        return new PlatformStudentGuardianSnapshot(
                studentId,
                guardianId,
                guardianId + "-user",
                name,
                name.toLowerCase().replace(' ', '.') + "@example.test",
                true,
                "Parent",
                official,
                true,
                false,
                true,
                true);
    }

    private static Student student(String externalId, String firstName, String lastName, int sequence) {
        Student student = new Student(INSTITUTION_ID, PLATFORM, externalId, firstName, lastName);
        setId(student, UUID.fromString("00000000-0000-0000-0000-00000000000" + sequence));
        return student;
    }

    private static void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class Store implements GuardianRepository, StudentGuardianRepository {
        private final Map<String, Student> students = new HashMap<>();
        private final Map<String, Guardian> guardians = new HashMap<>();
        private final Map<String, StudentGuardian> relationships = new HashMap<>();
        private int sequence = 10;

        private StudentRepository studentRepository() {
            return new StudentRepository() {
                @Override
                public Student save(Student student) {
                    return student;
                }

                @Override
                public Optional<Student> findStudentByInstitutionIdAndPlatformCodeAndExternalId(
                        UUID institutionId, String platformCode, String externalId) {
                    return Optional.ofNullable(students.get(externalId));
                }

                @Override
                public Optional<Student> findStudentById(UUID studentId) {
                    return students.values().stream()
                            .filter(student -> student.getId().equals(studentId))
                            .findFirst();
                }

                @Override
                public List<Student> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> studentIds) {
                    return students.values().stream()
                            .filter(student -> studentIds.contains(student.getId()))
                            .toList();
                }
            };
        }

        @Override
        public Guardian save(Guardian guardian) {
            if (guardian.getId() == null)
                setId(guardian, UUID.fromString("00000000-0000-0000-0000-0000000000" + sequence++));
            guardians.put(guardian.getExternalId(), guardian);
            return guardian;
        }

        @Override
        public Optional<Guardian> findByInstitutionIdAndPlatformCodeAndExternalId(
                UUID institutionId, String platformCode, String externalId) {
            return Optional.ofNullable(guardians.get(externalId));
        }

        @Override
        public List<Guardian> findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                UUID institutionId, String platformCode, Collection<String> externalIds) {
            return guardians.values().stream()
                    .filter(guardian -> externalIds.contains(guardian.getExternalId()))
                    .toList();
        }

        @Override
        public List<Guardian> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> guardianIds) {
            return guardians.values().stream()
                    .filter(guardian -> guardianIds.contains(guardian.getId()))
                    .toList();
        }

        @Override
        public StudentGuardian save(StudentGuardian relationship) {
            if (relationship.getId() == null)
                setId(relationship, UUID.fromString("00000000-0000-0000-0000-0000000000" + sequence++));
            relationships.put(relationship.getStudentId() + "|" + relationship.getGuardianId(), relationship);
            return relationship;
        }

        @Override
        public Optional<StudentGuardian> findByStudentIdAndGuardianId(UUID studentId, UUID guardianId) {
            return Optional.ofNullable(relationships.get(studentId + "|" + guardianId));
        }

        @Override
        public List<StudentGuardian> findByStudentId(UUID studentId) {
            return relationships.values().stream()
                    .filter(relationship -> studentId.equals(relationship.getStudentId()))
                    .toList();
        }
    }
}
