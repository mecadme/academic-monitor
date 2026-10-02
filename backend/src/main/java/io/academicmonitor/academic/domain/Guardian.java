package io.academicmonitor.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "guardians")
public class Guardian {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "platform_code", nullable = false, length = 32)
    private String platformCode;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "external_user_id", nullable = false, length = 128)
    private String externalUserId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(length = 320)
    private String email;

    @Column(name = "system_access", nullable = false)
    private boolean systemAccess;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Guardian() {}

    public Guardian(
            UUID institutionId,
            String platformCode,
            String externalId,
            String externalUserId,
            String displayName,
            String email,
            boolean systemAccess) {
        this.institutionId = requireId(institutionId, "institutionId");
        this.platformCode = requireText(platformCode, "platformCode");
        this.externalId = requireText(externalId, "externalId");
        this.externalUserId = requireText(externalUserId, "externalUserId");
        this.displayName = requireText(displayName, "displayName");
        this.email = optionalText(email);
        this.systemAccess = systemAccess;
    }

    public boolean updateContact(
            String expectedExternalUserId,
            String expectedDisplayName,
            String expectedEmail,
            boolean expectedSystemAccess) {
        String userId = requireText(expectedExternalUserId, "externalUserId");
        String name = requireText(expectedDisplayName, "displayName");
        String normalizedEmail = optionalText(expectedEmail);
        boolean changed = !externalUserId.equals(userId)
                || !displayName.equals(name)
                || !Objects.equals(email, normalizedEmail)
                || systemAccess != expectedSystemAccess;
        externalUserId = userId;
        displayName = name;
        email = normalizedEmail;
        systemAccess = expectedSystemAccess;
        return changed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public boolean hasSystemAccess() {
        return systemAccess;
    }

    private static UUID requireId(UUID value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
