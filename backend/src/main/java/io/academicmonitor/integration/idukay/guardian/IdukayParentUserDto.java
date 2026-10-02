package io.academicmonitor.integration.idukay.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayParentUserDto(
        @JsonProperty("_id") String id,
        String name,
        @JsonProperty("second_name") String secondName,
        String surname,
        @JsonProperty("second_surname") String secondSurname,
        String email,
        @JsonProperty("system_access") Boolean systemAccess) {}
