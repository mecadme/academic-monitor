package io.academicmonitor.integration.idukay.period;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayCustomYearDto(
        @JsonProperty("_id") String id,
        String name,
        String year,
        @JsonProperty("base_score") BigDecimal baseScore,
        List<IdukayTermDto> terms) {

    public IdukayCustomYearDto {
        terms = terms == null ? List.of() : List.copyOf(terms);
    }
}
