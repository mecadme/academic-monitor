package io.academicmonitor.monitoring.api;

import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.application.CommunicationResponse;
import io.academicmonitor.monitoring.application.AlertAttentionState;
import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.application.AlertInboxResponse;
import io.academicmonitor.monitoring.application.AlertTriageService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AlertCommunicationService alertCommunicationService;

    @Autowired
    public AlertInboxController(
            AlertInboxQueryService alertInboxQueryService,
            AlertTriageService alertTriageService,
            AlertCommunicationService alertCommunicationService) {
        this.alertInboxQueryService = alertInboxQueryService;
        this.alertTriageService = alertTriageService;
        this.alertCommunicationService = alertCommunicationService;
    }

    /** Retained for isolated inbox-controller tests that do not exercise communications. */
    public AlertInboxController(AlertInboxQueryService alertInboxQueryService, AlertTriageService alertTriageService) {
        this(alertInboxQueryService, alertTriageService, null);
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

    @PostMapping("/{alertId}/communication")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse prepareCommunication(
            @PathVariable UUID alertId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        if (alertCommunicationService == null) {
            throw new IllegalStateException("Communication workflow is unavailable");
        }
        return alertCommunicationService.prepare(institutionId, teacherUserId, alertId);
    }
}
