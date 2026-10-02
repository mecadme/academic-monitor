package io.academicmonitor.academic.api;

import io.academicmonitor.academic.application.StudentGuardianQueryService;
import io.academicmonitor.academic.application.StudentGuardianQueryService.StudentGuardianResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentGuardianController {
    private final StudentGuardianQueryService queryService;

    public StudentGuardianController(StudentGuardianQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{studentId}/guardians")
    public List<StudentGuardianResponse> guardians(
            @PathVariable UUID studentId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        return queryService.getGuardians(studentId, institutionId, teacherUserId);
    }
}
