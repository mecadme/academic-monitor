package io.academicmonitor.demo.api;

import io.academicmonitor.demo.application.DemoDashboardResult;
import io.academicmonitor.demo.application.DemoDashboardService;
import io.academicmonitor.demo.application.DemoScenario;
import io.academicmonitor.demo.application.DemoSyncResult;
import io.academicmonitor.demo.application.DemoSyncService;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/demo")
public class DemoSyncController {

    private final AuthenticatedAcademicContext context;
    private final DemoSyncService demoSyncService;
    private final DemoDashboardService dashboardService;

    public DemoSyncController(
            DemoSyncService demoSyncService,
            DemoDashboardService dashboardService,
            AuthenticatedAcademicContext context) {
        this.context = context;
        this.demoSyncService = demoSyncService;
        this.dashboardService = dashboardService;
    }

    @PostMapping("/sync")
    public DemoSyncResult sync(@RequestParam(defaultValue = "INITIAL") DemoScenario scenario) {
        return demoSyncService.sync(context.institutionId(), context.userId(), scenario);
    }

    @GetMapping("/dashboard")
    public DemoDashboardResult dashboard() {
        return dashboardService.getDashboard(context.institutionId(), context.userId());
    }
}
