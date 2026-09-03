package io.academicmonitor.monitoring.application;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Only open alerts can be triaged")
public class AlertTriageConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AlertTriageConflictException() {
        super("Only open alerts can be triaged");
    }
}
