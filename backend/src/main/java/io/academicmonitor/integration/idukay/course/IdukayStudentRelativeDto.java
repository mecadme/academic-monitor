package io.academicmonitor.integration.idukay.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayStudentRelativeDto(
        @JsonProperty("_id") String id,
        IdukayParentReferenceDto parent,
        String relationship,
        @JsonProperty("official_legal_guardian") Boolean officialLegalGuardian,
        @JsonProperty("is_legal_guardian") Boolean legalGuardian,
        @JsonProperty("economic_representative") Boolean economicRepresentative,
        @JsonProperty("can_pick_up") Boolean canPickUp,
        @JsonProperty("lives_with") Boolean livesWith) {}
