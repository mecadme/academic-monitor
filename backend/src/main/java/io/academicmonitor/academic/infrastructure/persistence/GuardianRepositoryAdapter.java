package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.academic.domain.StudentGuardian;
import io.academicmonitor.academic.domain.StudentGuardianRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class GuardianRepositoryAdapter implements GuardianRepository, StudentGuardianRepository {
    private final GuardianDataRepository guardianRepository;
    private final StudentGuardianDataRepository studentGuardianRepository;

    GuardianRepositoryAdapter(
            GuardianDataRepository guardianRepository, StudentGuardianDataRepository studentGuardianRepository) {
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
    }

    @Override
    public Guardian save(Guardian guardian) {
        return guardianRepository.save(guardian);
    }

    @Override
    public Optional<Guardian> findByInstitutionIdAndPlatformCodeAndExternalId(
            UUID institutionId, String platformCode, String externalId) {
        return guardianRepository.findByInstitutionIdAndPlatformCodeAndExternalId(
                institutionId, platformCode, externalId);
    }

    @Override
    public List<Guardian> findByInstitutionIdAndPlatformCodeAndExternalIdIn(
            UUID institutionId, String platformCode, Collection<String> externalIds) {
        return guardianRepository.findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                institutionId, platformCode, externalIds);
    }

    @Override
    public List<Guardian> findByInstitutionIdAndIdIn(UUID institutionId, Collection<UUID> guardianIds) {
        return guardianRepository.findByInstitutionIdAndIdIn(institutionId, guardianIds);
    }

    @Override
    public StudentGuardian save(StudentGuardian studentGuardian) {
        return studentGuardianRepository.save(studentGuardian);
    }

    @Override
    public Optional<StudentGuardian> findByStudentIdAndGuardianId(UUID studentId, UUID guardianId) {
        return studentGuardianRepository.findByStudentIdAndGuardianId(studentId, guardianId);
    }

    @Override
    public List<StudentGuardian> findByStudentId(UUID studentId) {
        return studentGuardianRepository.findByStudentId(studentId);
    }
}
