package io.academicmonitor.academic.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record AcademicPeriodCatalogResponse(UUID institutionId, UUID teacherUserId, List<AcademicPeriodItem> periods) {

    public AcademicPeriodCatalogResponse {
        periods = List.copyOf(periods);
    }

    public record AcademicPeriodItem(
            UUID id,
            String name,
            String abbreviation,
            int order,
            @JsonProperty("synchronized") boolean synchronizedPeriod) {}
}
