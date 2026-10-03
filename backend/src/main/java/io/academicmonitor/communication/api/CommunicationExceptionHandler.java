package io.academicmonitor.communication.api;

import io.academicmonitor.communication.application.CommunicationWorkflowException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CommunicationExceptionHandler {
    @ExceptionHandler(CommunicationWorkflowException.class)
    ProblemDetail handleWorkflow(CommunicationWorkflowException exception) {
        HttpStatus status =
                switch (exception.getError()) {
                    case ALERT_NOT_FOUND, COMMUNICATION_NOT_FOUND, STUDENT_OUT_OF_SCOPE -> HttpStatus.NOT_FOUND;
                    case ALERT_NOT_ACTIVE,
                            RECIPIENT_NOT_FOUND,
                            RECIPIENT_AMBIGUOUS,
                            RECIPIENT_NOT_REACHABLE,
                            COMMUNICATION_ALREADY_SENT,
                            COMMUNICATION_NOT_EDITABLE,
                            COMMUNICATION_NOT_SENDABLE -> HttpStatus.CONFLICT;
                };
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, "The requested communication action cannot be completed.");
        problem.setType(URI.create("urn:academic-monitor:communication:"
                + exception.getError().name().toLowerCase()));
        problem.setProperty("code", exception.getError().name());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The communication content is invalid.");
    }
}
