package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdukayLoginProfiles(String user, List<IdukayLoginProfile> profiles) {

    public IdukayLoginProfiles {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
