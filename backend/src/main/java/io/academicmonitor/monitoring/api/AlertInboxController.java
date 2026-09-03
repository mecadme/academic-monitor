package io.academicmonitor.monitoring.api;

import io.academicmonitor.monitoring.application.AlertAttentionState;
import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.application.AlertInboxResponse;
import io.academicmonitor.monitoring.application.AlertTriageService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertInboxController {

    private final AlertInboxQueryService alertInboxQueryService;
    private final AlertTriageService alertTriageService;

    public AlertInboxController(AlertInboxQueryService alertInboxQueryService, AlertTriageService alertTriageService) {
        this.alertInboxQueryService = alertInboxQueryService;
        this.alertTriageService = alertTriageService;
    }

    @GetMapping
    public AlertInboxResponse alerts(
            @RequestParam UUID institutionId,
            @RequestParam UUID teacherUserId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID academicPeriodId,
            @RequestParam(required = false, defaultValue = "ALL") AlertAttentionState attentionState) {
        return alertInboxQueryService.getInbox(
                institutionId, teacherUserId, courseId, academicPeriodId, attentionState);
    }

    @PostMapping("/{alertId}/acknowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledge(
            @PathVariable UUID alertId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        alertTriageService.acknowledge(institutionId, teacherUserId, alertId);
    }

    @PostMapping("/{alertId}/mark-pending")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markPending(
            @PathVariable UUID alertId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        alertTriageService.markPending(institutionId, teacherUserId, alertId);
    }
}
