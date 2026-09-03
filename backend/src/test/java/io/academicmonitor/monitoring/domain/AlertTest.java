package io.academicmonitor.monitoring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertTest {

    @Test
    void newAlertStartsOpenAndPending() {
        Alert alert = alert(AlertSeverity.WARNING, "6.50");

        assertTrue(alert.isOpen());
        assertTrue(alert.isPending());
        assertFalse(alert.isAcknowledged());
        assertNull(alert.getAcknowledgedAt());
    }

    @Test
    void acknowledgeIsIdempotentAndKeepsTheAlertOpen() {
        Alert alert = alert(AlertSeverity.WARNING, "6.50");

        assertTrue(alert.acknowledge());
        Instant acknowledgedAt = alert.getAcknowledgedAt();
        assertNotNull(acknowledgedAt);
        assertTrue(alert.isOpen());
        assertTrue(alert.isAcknowledged());

        assertFalse(alert.acknowledge());
        assertSame(acknowledgedAt, alert.getAcknowledgedAt());
    }

    @Test
    void markPendingIsIdempotentAndOnlyChangesAnOpenAcknowledgedAlert() {
        Alert alert = alert(AlertSeverity.WARNING, "6.50");
        alert.acknowledge();

        assertTrue(alert.markPending());
        assertTrue(alert.isPending());
        assertNull(alert.getAcknowledgedAt());
        assertFalse(alert.markPending());
    }

    @Test
    void resolvePreservesAcknowledgementForHistory() {
        Alert alert = alert(AlertSeverity.WARNING, "6.50");
        alert.acknowledge();
        Instant acknowledgedAt = alert.getAcknowledgedAt();

        alert.resolve();

        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertSame(acknowledgedAt, alert.getAcknowledgedAt());
        assertFalse(alert.isPending());
        assertFalse(alert.acknowledge());
        assertFalse(alert.markPending());
    }

    @Test
    void refreshPreservesAcknowledgementExceptWhenWarningEscalatesToCritical() {
        Alert warning = alert(AlertSeverity.WARNING, "6.50");
        warning.acknowledge();
        Instant warningAcknowledgedAt = warning.getAcknowledgedAt();
        warning.refresh(AlertSeverity.WARNING, new BigDecimal("6.00"));
        assertSame(warningAcknowledgedAt, warning.getAcknowledgedAt());

        warning.refresh(AlertSeverity.CRITICAL, new BigDecimal("4.50"));
        assertTrue(warning.isPending());
        assertEquals(new BigDecimal("4.50"), warning.getScoreSnapshot());

        Alert critical = alert(AlertSeverity.CRITICAL, "4.50");
        critical.acknowledge();
        Instant criticalAcknowledgedAt = critical.getAcknowledgedAt();
        critical.refresh(AlertSeverity.CRITICAL, new BigDecimal("4.00"));
        assertSame(criticalAcknowledgedAt, critical.getAcknowledgedAt());
        critical.refresh(AlertSeverity.WARNING, new BigDecimal("6.50"));
        assertSame(criticalAcknowledgedAt, critical.getAcknowledgedAt());
    }

    private static Alert alert(AlertSeverity severity, String score) {
        return new Alert(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "LOW_GRADE",
                severity,
                new BigDecimal(score));
    }
}
