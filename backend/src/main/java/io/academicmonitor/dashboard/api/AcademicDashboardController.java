package io.academicmonitor.dashboard.api;

import io.academicmonitor.dashboard.application.AcademicDashboardQueryService;
import io.academicmonitor.dashboard.application.AcademicDashboardResponse;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class AcademicDashboardController {
    private final AuthenticatedAcademicContext context;

    private final AcademicDashboardQueryService dashboardQueryService;

    public AcademicDashboardController(
            AcademicDashboardQueryService dashboardQueryService, AuthenticatedAcademicContext context) {
        this.context = context;
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping
    public AcademicDashboardResponse dashboard(@RequestParam(required = false) UUID academicPeriodId) {
        return dashboardQueryService.getDashboard(context.institutionId(), context.userId(), academicPeriodId);
    }
}
