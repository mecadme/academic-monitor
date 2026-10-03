package io.academicmonitor.communication.api;

import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.application.CommunicationResponse;
import io.academicmonitor.communication.domain.CommunicationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communications")
public class CommunicationController {
    private final AlertCommunicationService service;

    public CommunicationController(AlertCommunicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommunicationResponse> list(
            @RequestParam UUID institutionId,
            @RequestParam UUID teacherUserId,
            @RequestParam(required = false) CommunicationStatus status) {
        return service.list(institutionId, teacherUserId, status);
    }

    @GetMapping("/{communicationId}")
    public CommunicationResponse get(
            @PathVariable UUID communicationId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        return service.get(institutionId, teacherUserId, communicationId);
    }

    @PatchMapping("/{communicationId}")
    public CommunicationResponse edit(
            @PathVariable UUID communicationId,
            @RequestParam UUID institutionId,
            @RequestParam UUID teacherUserId,
            @RequestBody EditCommunicationRequest request) {
        return service.edit(institutionId, teacherUserId, communicationId, request.subject(), request.content());
    }

    @PostMapping("/{communicationId}/send")
    public CommunicationResponse send(
            @PathVariable UUID communicationId, @RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {
        return service.send(institutionId, teacherUserId, communicationId);
    }

    public record EditCommunicationRequest(String subject, String content) {}
}
