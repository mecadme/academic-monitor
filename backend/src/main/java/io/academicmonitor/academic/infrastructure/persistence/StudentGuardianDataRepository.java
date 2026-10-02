package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.StudentGuardian;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StudentGuardianDataRepository extends JpaRepository<StudentGuardian, UUID> {
    Optional<StudentGuardian> findByStudentIdAndGuardianId(UUID studentId, UUID guardianId);

    List<StudentGuardian> findByStudentId(UUID studentId);
}
