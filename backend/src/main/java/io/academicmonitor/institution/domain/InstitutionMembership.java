package io.academicmonitor.institution.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "institution_memberships")
public class InstitutionMembership {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_role", nullable = false, length = 32)
    private InstitutionRole institutionRole;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstitutionMembership() {
        // Required by JPA
    }

    public InstitutionMembership(UUID userId, UUID institutionId, InstitutionRole institutionRole) {

        this.userId = requireId(userId, "User id");
        this.institutionId = requireId(institutionId, "Institution id");
        this.institutionRole = requireRole(institutionRole);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public InstitutionRole getInstitutionRole() {
        return institutionRole;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void changeRole(InstitutionRole newRole) {
        this.institutionRole = requireRole(newRole);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static UUID requireId(UUID value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        return value;
    }

    private static InstitutionRole requireRole(InstitutionRole value) {
        if (value == null) {
            throw new IllegalArgumentException("Institution role must not be null");
        }

        return value;
    }
}
