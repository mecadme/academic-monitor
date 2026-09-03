package io.academicmonitor.monitoring.application;

import io.academicmonitor.monitoring.domain.Alert;

public enum AlertAttentionState {
    PENDING,
    ACKNOWLEDGED,
    ALL;

    boolean includes(Alert alert) {
        return switch (this) {
            case PENDING -> alert.isPending();
            case ACKNOWLEDGED -> alert.isAcknowledged();
            case ALL -> true;
        };
    }
}
