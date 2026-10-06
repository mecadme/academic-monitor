package io.academicmonitor.integration.idukay.test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request for the development-only direct Idukay transport check. */
public final class IdukayDirectTestNotificationRequest {

    @NotBlank
    @Size(max = 200)
    private final String subject;

    @NotBlank
    @Size(max = 20_000)
    private final String content;

    @JsonCreator
    public IdukayDirectTestNotificationRequest(
            @JsonProperty("subject") String subject, @JsonProperty("content") String content) {
        this.subject = subject;
        this.content = content;
    }

    /** Reject transport identifiers supplied by a client instead of silently ignoring them. */
    @JsonAnySetter
    void rejectUnexpectedField(String field, Object value) {
        throw new IllegalArgumentException("unexpected direct notification field");
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }
}
