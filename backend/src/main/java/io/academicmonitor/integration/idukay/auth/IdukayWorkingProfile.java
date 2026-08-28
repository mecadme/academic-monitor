package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record IdukayWorkingProfile(@JsonProperty("_id") String id, @JsonProperty("collection_name") String collectionName) {}
