package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record IdukayUserPreferences(
        @JsonProperty("working_year") IdukayPreferenceReference workingYear,
        @JsonProperty("working_school") IdukayPreferenceReference workingSchool,
        @JsonProperty("working_organization") IdukayPreferenceReference workingOrganization,
        @JsonProperty("selected_student") IdukayPreferenceReference selectedStudent,
        @JsonProperty("working_profile") IdukayWorkingProfile workingProfile,
        @JsonProperty("time_zone") String timeZone) {}
