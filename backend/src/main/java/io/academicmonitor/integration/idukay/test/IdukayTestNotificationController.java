package io.academicmonitor.integration.idukay.test;

import io.academicmonitor.communication.application.ManualNotificationFailure;
import io.academicmonitor.communication.application.ManualNotificationRequest;
import io.academicmonitor.communication.application.ManualNotificationResult;
import io.academicmonitor.communication.application.ManualNotificationService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/idukay")
@ConditionalOnProperty(prefix = "app.idukay", name = "test-login-enabled", havingValue = "true")
public class IdukayTestNotificationController {

    private final ManualNotificationService service;
    private final IdukayDirectTestNotificationService directService;

    public IdukayTestNotificationController(
            ManualNotificationService service, IdukayDirectTestNotificationService directService) {
        this.service = service;
        this.directService = directService;
    }

    @PostMapping("/test-notification-direct")
    public ResponseEntity<IdukayDirectTestNotificationResponse> sendDirect(
            @Valid @RequestBody IdukayDirectTestNotificationRequest request) {
        IdukayDirectTestNotificationResult result = directService.send(new IdukayDirectTestNotificationCommand(
                request.getInstitutionId(), request.getSubject(), request.getContent()));
        if (result.sent()) {
            return ResponseEntity.ok(new IdukayDirectTestNotificationResponse("SENT"));
        }
        return ResponseEntity.status(statusFor(result.failure()))
                .body(new IdukayDirectTestNotificationResponse(result.failure().name()));
    }

    @PostMapping("/test-notification")
    public ResponseEntity<IdukayTestNotificationResponse> send(
            @Valid @RequestBody IdukayTestNotificationRequest request) {
        ManualNotificationResult result = service.send(new ManualNotificationRequest(
                request.institutionId(),
                request.teacherUserId(),
                request.studentId(),
                request.subject(),
                request.content()));
        if (result.succeeded()) {
            return ResponseEntity.ok(new IdukayTestNotificationResponse(result.communicationId(), "SENT"));
        }
        return ResponseEntity.status(statusFor(result.failure()))
                .body(new IdukayTestNotificationResponse(
                        result.communicationId(), result.failure().name()));
    }

    private static HttpStatus statusFor(ManualNotificationFailure failure) {
        return switch (failure) {
            case STUDENT_OUT_OF_SCOPE -> HttpStatus.NOT_FOUND;
            case RECIPIENT_AMBIGUOUS -> HttpStatus.CONFLICT;
            case PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case PROVIDER_REJECTED -> HttpStatus.BAD_GATEWAY;
            case RECIPIENT_NOT_FOUND, RECIPIENT_NOT_REACHABLE, SENDER_NOT_RESOLVED -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    private static HttpStatus statusFor(IdukayDirectTestNotificationFailure failure) {
        return switch (failure) {
            case TEST_RECIPIENT_NOT_CONFIGURED, SENDER_NOT_RESOLVED -> HttpStatus.UNPROCESSABLE_CONTENT;
            case PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case PROVIDER_REJECTED -> HttpStatus.BAD_GATEWAY;
        };
    }
}
