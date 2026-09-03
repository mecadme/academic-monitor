package io.academicmonitor.monitoring.application;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Alert not found")
public class AlertTriageNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AlertTriageNotFoundException() {
        super("Alert not found");
    }
}
