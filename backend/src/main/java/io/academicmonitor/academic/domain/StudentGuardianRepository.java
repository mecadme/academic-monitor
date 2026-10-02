package io.academicmonitor.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentGuardianRepository {
    StudentGuardian save(StudentGuardian studentGuardian);

    Optional<StudentGuardian> findByStudentIdAndGuardianId(UUID studentId, UUID guardianId);

    List<StudentGuardian> findByStudentId(UUID studentId);
}
