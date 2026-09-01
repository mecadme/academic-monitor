package io.academicmonitor.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "platform_code", nullable = false, length = 32, updatable = false)
    private String platformCode;

    @Column(name = "external_id", nullable = false, length = 128, updatable = false)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "external_year", length = 32)
    private String externalYear;

    @Column(name = "base_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AcademicYear() {}

    public AcademicYear(
            UUID institutionId,
            String platformCode,
            String externalId,
            String name,
            String externalYear,
            BigDecimal baseScore) {
        this.institutionId = requireId(institutionId, "institutionId");
        this.platformCode = requireText(platformCode, "platformCode");
        this.externalId = requireText(externalId, "externalId");
        this.name = requireText(name, "name");
        this.externalYear = optionalText(externalYear);
        this.baseScore = requirePositive(baseScore, "baseScore");
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getExternalYear() {
        return externalYear;
    }

    public BigDecimal getBaseScore() {
        return baseScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static UUID requireId(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }
}
