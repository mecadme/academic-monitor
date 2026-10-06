package io.academicmonitor.monitoring.api;

import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.application.CommunicationResponse;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
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
    private final AuthenticatedAcademicContext context;

    private final AlertInboxQueryService alertInboxQueryService;
    private final AlertTriageService alertTriageService;
    private final AlertCommunicationService alertCommunicationService;

    @Autowired
    public AlertInboxController(
            AlertInboxQueryService alertInboxQueryService,
            AlertTriageService alertTriageService,
            AlertCommunicationService alertCommunicationService,
            AuthenticatedAcademicContext context) {
        this.context = context;
        this.alertInboxQueryService = alertInboxQueryService;
        this.alertTriageService = alertTriageService;
        this.alertCommunicationService = alertCommunicationService;
    }

    @GetMapping
    public AlertInboxResponse alerts(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID academicPeriodId,
            @RequestParam(required = false, defaultValue = "ALL") AlertAttentionState attentionState) {
        return alertInboxQueryService.getInbox(
                context.institutionId(), context.userId(), courseId, academicPeriodId, attentionState);
    }

    @PostMapping("/{alertId}/acknowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledge(@PathVariable UUID alertId) {
        alertTriageService.acknowledge(context.institutionId(), context.userId(), alertId);
    }

    @PostMapping("/{alertId}/mark-pending")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markPending(@PathVariable UUID alertId) {
        alertTriageService.markPending(context.institutionId(), context.userId(), alertId);
    }

    @PostMapping("/{alertId}/communication")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse prepareCommunication(@PathVariable UUID alertId) {
        if (alertCommunicationService == null) {
            throw new IllegalStateException("Communication workflow is unavailable");
        }
        return alertCommunicationService.prepare(context.institutionId(), context.userId(), alertId);
    }
}
