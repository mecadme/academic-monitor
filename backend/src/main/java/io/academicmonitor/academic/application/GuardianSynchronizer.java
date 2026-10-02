package io.academicmonitor.academic.application;

import io.academicmonitor.academic.application.port.PlatformGuardianSyncSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentGuardianSnapshot;
import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.academic.domain.Student;
import io.academicmonitor.academic.domain.StudentGuardian;
import io.academicmonitor.academic.domain.StudentGuardianRepository;
import io.academicmonitor.academic.domain.StudentRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GuardianSynchronizer {
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    public GuardianSynchronizer(
            StudentRepository studentRepository,
            GuardianRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository) {
        this.studentRepository = studentRepository;
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
    }

    public Result synchronize(
            UUID institutionId,
            String platformCode,
            Collection<String> scopedStudentExternalIds,
            PlatformGuardianSyncSnapshot snapshot) {
        if (snapshot == null || scopedStudentExternalIds == null || scopedStudentExternalIds.isEmpty())
            return new Result(0, 0, snapshot == null ? 0 : snapshot.warnings());
        Map<String, Student> studentsByExternalId = new HashMap<>();
        for (String externalId : scopedStudentExternalIds) {
            if (externalId != null && !externalId.isBlank())
                studentRepository
                        .findStudentByInstitutionIdAndPlatformCodeAndExternalId(institutionId, platformCode, externalId)
                        .ifPresent(student -> studentsByExternalId.put(externalId.trim(), student));
        }
        int guardiansUpserted = 0;
        int relationshipsUpserted = 0;
        int warnings = snapshot.warnings();
        Set<String> guardianExternalIds = snapshot.relationships().stream()
                .filter(java.util.Objects::nonNull)
                .map(PlatformStudentGuardianSnapshot::guardianExternalId)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
        Collection<Guardian> persistedGuardians = guardianExternalIds.isEmpty()
                ? java.util.List.of()
                : guardianRepository.findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                        institutionId, platformCode, guardianExternalIds);
        Map<String, Guardian> guardiansByExternalId = persistedGuardians.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Guardian::getExternalId, guardian -> guardian, (first, ignored) -> first, HashMap::new));
        Set<String> processedRelationships = new HashSet<>();
        for (PlatformStudentGuardianSnapshot relationship : snapshot.relationships()) {
            if (relationship == null) {
                warnings++;
                continue;
            }
            Student student = studentsByExternalId.get(normalize(relationship.studentExternalId()));
            if (student == null
                    || isBlank(relationship.guardianExternalId())
                    || isBlank(relationship.guardianUserExternalId())
                    || isBlank(relationship.displayName())) {
                warnings++;
                continue;
            }
            String relationshipKey =
                    student.getId() + "|" + relationship.guardianExternalId().trim();
            if (!processedRelationships.add(relationshipKey)) continue;
            String guardianExternalId = relationship.guardianExternalId().trim();
            Guardian guardian = guardiansByExternalId.get(guardianExternalId);
            if (guardian == null) {
                guardian = guardianRepository.save(new Guardian(
                        institutionId,
                        platformCode,
                        relationship.guardianExternalId(),
                        relationship.guardianUserExternalId(),
                        relationship.displayName(),
                        relationship.email(),
                        relationship.systemAccess()));
                guardiansUpserted++;
                guardiansByExternalId.put(guardianExternalId, guardian);
            } else if (guardian.updateContact(
                    relationship.guardianUserExternalId(),
                    relationship.displayName(),
                    relationship.email(),
                    relationship.systemAccess())) {
                guardianRepository.save(guardian);
                guardiansUpserted++;
            }
            StudentGuardian studentGuardian = studentGuardianRepository
                    .findByStudentIdAndGuardianId(student.getId(), guardian.getId())
                    .orElse(null);
            if (studentGuardian == null) {
                studentGuardianRepository.save(new StudentGuardian(
                        institutionId,
                        student.getId(),
                        guardian.getId(),
                        relationship.relationship(),
                        relationship.officialLegalGuardian(),
                        relationship.legalGuardian(),
                        relationship.economicRepresentative(),
                        relationship.canPickUp(),
                        relationship.livesWithStudent()));
                relationshipsUpserted++;
            } else if (studentGuardian.updateDetails(
                    relationship.relationship(),
                    relationship.officialLegalGuardian(),
                    relationship.legalGuardian(),
                    relationship.economicRepresentative(),
                    relationship.canPickUp(),
                    relationship.livesWithStudent())) {
                studentGuardianRepository.save(studentGuardian);
                relationshipsUpserted++;
            }
        }
        return new Result(
                guardiansUpserted,
                relationshipsUpserted,
                warnings,
                snapshot.studentsInspected(),
                snapshot.guardianStudentFetches(),
                snapshot.guardianStudentWarnings(),
                snapshot.guardianStudentFetchDurationMs(),
                snapshot.uniqueParentIds(),
                snapshot.guardianCacheHits(),
                snapshot.guardianFetches(),
                snapshot.guardianSyncDurationMs());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public record Result(
            int guardiansUpserted,
            int guardianRelationshipsUpserted,
            int guardianWarnings,
            int studentsInspected,
            int guardianStudentFetches,
            int guardianStudentWarnings,
            long guardianStudentFetchDurationMs,
            int uniqueParentIds,
            int guardianCacheHits,
            int guardianFetches,
            long guardianSyncDurationMs) {
        public Result(int guardiansUpserted, int guardianRelationshipsUpserted, int guardianWarnings) {
            this(guardiansUpserted, guardianRelationshipsUpserted, guardianWarnings, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
