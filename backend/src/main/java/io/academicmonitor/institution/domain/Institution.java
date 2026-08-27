package io.academicmonitor.institution.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "America/Guayaquil";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Institution() {}

    public Institution(String name, String timezone) {
        this.name = requireName(name);
        this.timezone = requireTimezone(timezone);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTimezone() {
        return timezone;
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

    public void rename(String newName) {
        this.name = requireName(newName);
    }

    public void changeTimezone(String newTimezone) {
        this.timezone = requireTimezone(newTimezone);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Institution name must not be blank");
        }

        String normalized = value.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException("Institution name must not exceed 150 characters");
        }

        return normalized;
    }

    private static String requireTimezone(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Institution timezone must not be blank");
        }

        String normalized = value.trim();

        if (normalized.length() > 64) {
            throw new IllegalArgumentException("Institution timezone must not exceed 64 characters");
        }

        return normalized;
    }
}
