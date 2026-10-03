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
            UUID academicYearId,
            String externalId,
            String name,
            String abbreviation,
            int order,
            String academicYear,
            @JsonProperty("synchronized") boolean synchronizedPeriod) {}
}
