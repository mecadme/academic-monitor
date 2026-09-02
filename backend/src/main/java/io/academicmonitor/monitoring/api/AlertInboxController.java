package io.academicmonitor.monitoring.api;

import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.application.AlertInboxResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertInboxController {

    private final AlertInboxQueryService alertInboxQueryService;

    public AlertInboxController(AlertInboxQueryService alertInboxQueryService) {
        this.alertInboxQueryService = alertInboxQueryService;
    }

    @GetMapping
    public AlertInboxResponse alerts(
            @RequestParam UUID institutionId,
            @RequestParam UUID teacherUserId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID academicPeriodId) {
        return alertInboxQueryService.getInbox(institutionId, teacherUserId, courseId, academicPeriodId);
    }
}
