package io.academicmonitor.communication.application;

public class CommunicationWorkflowException extends RuntimeException {
    private final CommunicationWorkflowError error;

    public CommunicationWorkflowException(CommunicationWorkflowError error) {
        super(error.name());
        this.error = error;
    }

    public CommunicationWorkflowError getError() {
        return error;
    }
}
