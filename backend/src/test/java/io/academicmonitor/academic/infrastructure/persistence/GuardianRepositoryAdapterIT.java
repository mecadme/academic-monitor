package io.academicmonitor.academic.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentGuardian;
import io.academicmonitor.academic.domain.StudentGuardianRepository;
import io.academicmonitor.academic.domain.StudentRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GuardianRepositoryAdapterIT extends PostgresIntegrationTest {
    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private StudentGuardianRepository studentGuardianRepository;

    @Test
    void isolatesGuardiansByInstitutionAndPersistsStudentRelationships() {
        Institution firstInstitution =
                institutionRepository.save(new Institution("First Test Institution", "America/Guayaquil"));
        Institution secondInstitution =
                institutionRepository.save(new Institution("Second Test Institution", "America/Guayaquil"));
        Guardian firstGuardian = guardianRepository.save(new Guardian(
                firstInstitution.getId(),
                "TEST",
                "guardian-profile-alpha",
                "guardian-user-alpha",
                "Guardian One",
                null,
                true));
        Guardian secondGuardian = guardianRepository.save(new Guardian(
                secondInstitution.getId(),
                "TEST",
                "guardian-profile-alpha",
                "guardian-user-beta",
                "Guardian Two",
                null,
                false));
        Student student = studentRepository.save(
                new Student(firstInstitution.getId(), "TEST", "student-alpha", "Student", "Alpha"));
        StudentGuardian relationship = studentGuardianRepository.save(new StudentGuardian(
                firstInstitution.getId(),
                student.getId(),
                firstGuardian.getId(),
                "Parent",
                true,
                true,
                false,
                true,
                true));

        assertNotNull(relationship.getId());
        assertEquals(
                firstGuardian.getId(),
                guardianRepository
                        .findByInstitutionIdAndPlatformCodeAndExternalId(
                                firstInstitution.getId(), "TEST", "guardian-profile-alpha")
                        .orElseThrow()
                        .getId());
        assertEquals(
                secondGuardian.getId(),
                guardianRepository
                        .findByInstitutionIdAndPlatformCodeAndExternalId(
                                secondInstitution.getId(), "TEST", "guardian-profile-alpha")
                        .orElseThrow()
                        .getId());
        assertEquals(
                List.of(firstGuardian.getId()),
                guardianRepository
                        .findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                                firstInstitution.getId(), "TEST", List.of("guardian-profile-alpha"))
                        .stream()
                        .map(Guardian::getId)
                        .toList());
        assertEquals(
                1, studentGuardianRepository.findByStudentId(student.getId()).size());
    }
}
