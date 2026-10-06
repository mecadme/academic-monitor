package io.academicmonitor.communication.api;

import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.application.CommunicationResponse;
import io.academicmonitor.communication.domain.CommunicationStatus;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communications")
public class CommunicationController {
    private final AlertCommunicationService service;
    private final AuthenticatedAcademicContext context;

    public CommunicationController(AlertCommunicationService service, AuthenticatedAcademicContext context) {
        this.service = service;
        this.context = context;
    }

    @DeleteMapping("/{communicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID communicationId) {
        service.deleteDraft(context.institutionId(), context.userId(), communicationId);
    }

    @GetMapping
    public List<CommunicationResponse> list(@RequestParam(required = false) CommunicationStatus status) {
        return service.list(context.institutionId(), context.userId(), status);
    }

    @GetMapping("/{communicationId}")
    public CommunicationResponse get(@PathVariable UUID communicationId) {
        return service.get(context.institutionId(), context.userId(), communicationId);
    }

    @PatchMapping("/{communicationId}")
    public CommunicationResponse edit(
            @PathVariable UUID communicationId, @RequestBody EditCommunicationRequest request) {
        return service.edit(
                context.institutionId(), context.userId(), communicationId, request.subject(), request.content());
    }

    @PostMapping("/{communicationId}/send")
    public CommunicationResponse send(@PathVariable UUID communicationId) {
        return service.send(context.institutionId(), context.userId(), communicationId);
    }

    public record EditCommunicationRequest(String subject, String content) {}
}
