package io.academicmonitor.identity.application;

import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.institution.domain.InstitutionRole;
import java.util.UUID;

public record AcademicMonitorPrincipal(
        UUID userId, UUID institutionId, SystemRole systemRole, InstitutionRole institutionRole) {}
