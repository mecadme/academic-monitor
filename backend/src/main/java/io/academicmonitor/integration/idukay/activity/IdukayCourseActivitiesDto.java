package io.academicmonitor.integration.idukay.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayCourseActivitiesDto(@JsonProperty("_id") String id, List<IdukayActivityDto> activities) {

    public IdukayCourseActivitiesDto {
        activities = activities == null ? List.of() : List.copyOf(activities);
    }
}
