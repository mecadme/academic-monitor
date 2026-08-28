package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayLoginContexts(
        JsonNode user,
        List<IdukayLoginSchool> schools,
        JsonNode organization,
        @JsonProperty("is_admin") boolean admin,
        List<IdukayLoginProfile> profiles) {

    public IdukayLoginContexts {

        schools = schools == null ? List.of() : List.copyOf(schools);

        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
