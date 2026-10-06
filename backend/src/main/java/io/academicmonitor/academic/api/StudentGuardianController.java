package io.academicmonitor.academic.api;

import io.academicmonitor.academic.application.StudentGuardianQueryService;
import io.academicmonitor.academic.application.StudentGuardianQueryService.StudentGuardianResponse;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentGuardianController {
    private final AuthenticatedAcademicContext context;
    private final StudentGuardianQueryService queryService;

    public StudentGuardianController(StudentGuardianQueryService queryService, AuthenticatedAcademicContext context) {
        this.context = context;
        this.queryService = queryService;
    }

    @GetMapping("/{studentId}/guardians")
    public List<StudentGuardianResponse> guardians(@PathVariable UUID studentId) {
        return queryService.getGuardians(studentId, context.institutionId(), context.userId());
    }
}
