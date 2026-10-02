package io.academicmonitor.integration.idukay.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayParentDto(
        @JsonProperty("_id") String id,
        IdukayParentUserDto user,
        @JsonProperty("system_access") Boolean systemAccess,
        @JsonProperty("relational_data") IdukayParentRelationalDataDto relationalData) {}
