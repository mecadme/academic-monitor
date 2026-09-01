package io.academicmonitor.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "academic_periods")
public class AcademicPeriod {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private UUID academicYearId;

    @Column(name = "external_id", nullable = false, length = 128, updatable = false)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 64)
    private String abbreviation;

    @Column(name = "period_order", nullable = false)
    private int order;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AcademicPeriod() {}

    public AcademicPeriod(UUID academicYearId, String externalId, String name, String abbreviation, int order) {
        this.academicYearId = requireId(academicYearId, "academicYearId");
        this.externalId = requireText(externalId, "externalId");
        this.name = requireText(name, "name");
        this.abbreviation = optionalText(abbreviation);
        if (order < 1) {
            throw new IllegalArgumentException("order must be greater than zero");
        }
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public int getOrder() {
        return order;
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
}
