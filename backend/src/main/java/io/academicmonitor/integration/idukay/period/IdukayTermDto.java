package io.academicmonitor.integration.idukay.period;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayTermDto(
    @JsonProperty("_id") String id,
    String name,
    String abbreviation,
    List<IdukayPartDto> parts) {

    public IdukayTermDto {
        parts = parts == null
            ? List.of()
            : List.copyOf(parts);
    }
}
