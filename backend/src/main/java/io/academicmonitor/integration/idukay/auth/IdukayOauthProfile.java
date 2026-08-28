package io.academicmonitor.integration.idukay.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IdukayOauthProfile(
        @JsonProperty("collection_name") String collectionName, @JsonProperty("_id") String id, String user) {

    public IdukayOauthProfile {
        collectionName = requireText(collectionName, "collectionName");
        id = requireText(id, "id");
        user = requireText(user, "user");
    }

    public static IdukayOauthProfile from(IdukayLoginProfile profile, String userId) {

        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }

        return new IdukayOauthProfile(profile.collectionName(), profile.id(), userId);
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }
}
