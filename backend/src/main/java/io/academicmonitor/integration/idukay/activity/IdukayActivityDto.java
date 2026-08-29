package io.academicmonitor.integration.idukay.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayActivityDto(
        @JsonProperty("_id") String id, String name, Long date, List<IdukayActivityScoreDto> scores) {

    public IdukayActivityDto {
        scores = scores == null ? List.of() : List.copyOf(scores);
    }
}
