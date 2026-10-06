package io.academicmonitor.identity.application;

import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.institution.domain.InstitutionRole;
import java.util.UUID;

public record AuthView(UserView user, InstitutionView institution) {
    public record UserView(UUID id, String email, SystemRole systemRole) {}

    public record InstitutionView(UUID id, String name, InstitutionRole role) {}
}
