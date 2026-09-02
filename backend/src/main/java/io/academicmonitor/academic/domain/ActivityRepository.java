package io.academicmonitor.academic.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository {

    Activity save(Activity activity);

    Optional<Activity> findByCourseIdAndPlatformCodeAndExternalId(
            UUID courseId, String platformCode, String externalId);

    Optional<Activity> findLatestByCourseId(UUID courseId);

    List<Activity> findActivitiesByCourseIdIn(Collection<UUID> courseIds);
}
