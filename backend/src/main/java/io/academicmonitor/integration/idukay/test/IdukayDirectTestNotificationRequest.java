package io.academicmonitor.integration.idukay.test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request for the development-only direct Idukay transport check. */
public final class IdukayDirectTestNotificationRequest {

    @NotNull
    private final UUID institutionId;

    @NotBlank
    @Size(max = 200)
    private final String subject;

    @NotBlank
    @Size(max = 20_000)
    private final String content;

    @JsonCreator
    public IdukayDirectTestNotificationRequest(
            @JsonProperty("institutionId") UUID institutionId,
            @JsonProperty("subject") String subject,
            @JsonProperty("content") String content) {
        this.institutionId = institutionId;
        this.subject = subject;
        this.content = content;
    }

    /** Reject transport identifiers supplied by a client instead of silently ignoring them. */
    @JsonAnySetter
    void rejectUnexpectedField(String field, Object value) {
        throw new IllegalArgumentException("unexpected direct notification field");
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }
}
