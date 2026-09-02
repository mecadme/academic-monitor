package io.academicmonitor.dashboard.api;

import io.academicmonitor.dashboard.application.AcademicDashboardQueryService;
import io.academicmonitor.dashboard.application.AcademicDashboardResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class AcademicDashboardController {

    private final AcademicDashboardQueryService dashboardQueryService;

    public AcademicDashboardController(AcademicDashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping
    public AcademicDashboardResponse dashboard(@RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        return dashboardQueryService.getDashboard(institutionId, teacherUserId);
    }
}
