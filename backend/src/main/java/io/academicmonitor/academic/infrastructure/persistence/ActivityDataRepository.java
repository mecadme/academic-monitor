package io.academicmonitor.academic.infrastructure.persistence;

import io.academicmonitor.academic.domain.Activity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ActivityDataRepository extends JpaRepository<Activity, UUID> {

    Optional<Activity> findByCourseIdAndPlatformCodeAndExternalId(
            UUID courseId, String platformCode, String externalId);

    Optional<Activity> findTopByCourseIdOrderByDueDateDesc(UUID courseId);
}
