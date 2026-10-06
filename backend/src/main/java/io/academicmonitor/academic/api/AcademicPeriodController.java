package io.academicmonitor.academic.api;

import io.academicmonitor.academic.application.AcademicPeriodCatalogQueryService;
import io.academicmonitor.academic.application.AcademicPeriodCatalogResponse;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academic-periods")
public class AcademicPeriodController {
    private final AuthenticatedAcademicContext context;

    private final AcademicPeriodCatalogQueryService queryService;

    public AcademicPeriodController(
            AcademicPeriodCatalogQueryService queryService, AuthenticatedAcademicContext context) {
        this.context = context;
        this.queryService = queryService;
    }

    @GetMapping
    public AcademicPeriodCatalogResponse periods() {
        return queryService.getPeriods(context.institutionId(), context.userId());
    }
}
