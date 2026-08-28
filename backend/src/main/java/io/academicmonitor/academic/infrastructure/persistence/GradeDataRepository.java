package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.Grade;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GradeDataRepository extends JpaRepository<Grade, UUID> {

    Optional<Grade> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
}
