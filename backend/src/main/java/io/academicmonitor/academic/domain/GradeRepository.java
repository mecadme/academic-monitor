package io.academicmonitor.academic.domain;

import java.util.Optional;
import java.util.UUID;

public interface GradeRepository {

    Grade save(Grade grade);

    Optional<Grade> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
}
