package io.academicmonitor.integration.idukay.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayStudentDto(
        @JsonProperty("_id") String id,
        @JsonProperty("relational_data") IdukayStudentRelationalDataDto relationalData,
        List<IdukayStudentRelativeDto> relatives) {
    public IdukayStudentDto {
        relatives = relatives == null ? List.of() : List.copyOf(relatives);
    }
}
