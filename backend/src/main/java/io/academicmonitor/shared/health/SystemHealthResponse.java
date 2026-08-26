package io.academicmonitor.shared.health;

import java.time.Instant;
import java.util.List;

public record SystemHealthResponse(ServiceState status, Instant checkedAt, List<ServiceHealth> services) {

    public static SystemHealthResponse from(List<ServiceHealth> services) {
        boolean allUp = services.stream().allMatch(ServiceHealth::isUp);
        return new SystemHealthResponse(
                allUp ? ServiceState.UP : ServiceState.DEGRADED, Instant.now(), List.copyOf(services));
    }
}
