package io.academicmonitor.academic.api;

import io.academicmonitor.academic.application.AcademicPeriodCatalogQueryService;
import io.academicmonitor.academic.application.AcademicPeriodCatalogResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academic-periods")
public class AcademicPeriodController {

    private final AcademicPeriodCatalogQueryService queryService;

    public AcademicPeriodController(AcademicPeriodCatalogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public AcademicPeriodCatalogResponse periods(@RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        return queryService.getPeriods(institutionId, teacherUserId);
    }
}
