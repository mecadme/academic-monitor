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
@Table(name = "student_guardians")
public class StudentGuardian {

    @Id
    @Generated
    @ColumnDefault("uuidv7()")
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "institution_id", nullable = false, updatable = false)
    private UUID institutionId;

    @Column(name = "guardian_id", nullable = false, updatable = false)
    private UUID guardianId;

    @Column(length = 100)
    private String relationship;

    @Column(name = "official_legal_guardian", nullable = false)
    private boolean officialLegalGuardian;

    @Column(name = "legal_guardian", nullable = false)
    private boolean legalGuardian;

    @Column(name = "economic_representative", nullable = false)
    private boolean economicRepresentative;

    @Column(name = "can_pick_up", nullable = false)
    private boolean canPickUp;

    @Column(name = "lives_with_student", nullable = false)
    private boolean livesWithStudent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentGuardian() {}

    public StudentGuardian(
            UUID institutionId,
            UUID studentId,
            UUID guardianId,
            String relationship,
            boolean officialLegalGuardian,
            boolean legalGuardian,
            boolean economicRepresentative,
            boolean canPickUp,
            boolean livesWithStudent) {
        this.institutionId = requireId(institutionId, "institutionId");
        this.studentId = requireId(studentId, "studentId");
        this.guardianId = requireId(guardianId, "guardianId");
        updateDetails(
                relationship,
                officialLegalGuardian,
                legalGuardian,
                economicRepresentative,
                canPickUp,
                livesWithStudent);
    }

    public boolean updateDetails(
            String expectedRelationship,
            boolean expectedOfficialLegalGuardian,
            boolean expectedLegalGuardian,
            boolean expectedEconomicRepresentative,
            boolean expectedCanPickUp,
            boolean expectedLivesWithStudent) {
        String normalizedRelationship = optionalText(expectedRelationship);
        boolean changed = !Objects.equals(relationship, normalizedRelationship)
                || officialLegalGuardian != expectedOfficialLegalGuardian
                || legalGuardian != expectedLegalGuardian
                || economicRepresentative != expectedEconomicRepresentative
                || canPickUp != expectedCanPickUp
                || livesWithStudent != expectedLivesWithStudent;
        relationship = normalizedRelationship;
        officialLegalGuardian = expectedOfficialLegalGuardian;
        legalGuardian = expectedLegalGuardian;
        economicRepresentative = expectedEconomicRepresentative;
        canPickUp = expectedCanPickUp;
        livesWithStudent = expectedLivesWithStudent;
        return changed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public UUID getGuardianId() {
        return guardianId;
    }

    public String getRelationship() {
        return relationship;
    }

    public boolean isOfficialLegalGuardian() {
        return officialLegalGuardian;
    }

    public boolean isLegalGuardian() {
        return legalGuardian;
    }

    public boolean isEconomicRepresentative() {
        return economicRepresentative;
    }

    public boolean canPickUp() {
        return canPickUp;
    }

    public boolean livesWithStudent() {
        return livesWithStudent;
    }

    private static UUID requireId(UUID value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
