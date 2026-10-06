package io.academicmonitor.identity.application;

import io.academicmonitor.institution.domain.InstitutionRole;
import java.util.List;
import java.util.UUID;

public class InstitutionSelectionRequired extends RuntimeException {
    private final List<InstitutionChoice> institutions;

    public InstitutionSelectionRequired(List<InstitutionChoice> institutions) {
        super("Selecciona una institución para continuar.");
        this.institutions = List.copyOf(institutions);
    }

    public List<InstitutionChoice> institutions() {
        return institutions;
    }

    public record InstitutionChoice(UUID institutionId, String institutionName, InstitutionRole institutionRole) {}
}
