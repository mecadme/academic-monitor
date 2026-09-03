package io.academicmonitor.monitoring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationServiceTest {

    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID ACTIVITY_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock
    private AlertRepository alertRepository;

    @Test
    void lowGradeRefreshFindsAcknowledgedOpenAlertAndOnlyEscalationMakesItPendingAgain() {
        Alert alert = alert(AlertSeverity.WARNING, "6.50");
        alert.acknowledge();
        var originalAcknowledgement = alert.getAcknowledgedAt();
        when(alertRepository.findByActivityIdAndStudentIdAndRuleCodeAndStatus(
                        ACTIVITY_ID, STUDENT_ID, "LOW_GRADE", AlertStatus.OPEN))
                .thenReturn(Optional.of(alert));
        AlertEvaluationService service = new AlertEvaluationService(alertRepository);

        service.evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("6.00"));

        assertSame(originalAcknowledgement, alert.getAcknowledgedAt());
        assertEquals(AlertSeverity.WARNING, alert.getSeverity());
        assertEquals(new BigDecimal("6.00"), alert.getScoreSnapshot());

        service.evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("4.50"));

        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.isPending());
        verify(alertRepository, org.mockito.Mockito.times(2)).save(alert);
    }

    @Test
    void acknowledgedCriticalRefreshAndDeescalationPreserveAcknowledgement() {
        Alert alert = alert(AlertSeverity.CRITICAL, "4.50");
        alert.acknowledge();
        var originalAcknowledgement = alert.getAcknowledgedAt();
        when(alertRepository.findByActivityIdAndStudentIdAndRuleCodeAndStatus(
                        ACTIVITY_ID, STUDENT_ID, "LOW_GRADE", AlertStatus.OPEN))
                .thenReturn(Optional.of(alert));
        AlertEvaluationService service = new AlertEvaluationService(alertRepository);

        service.evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("4.00"));
        assertSame(originalAcknowledgement, alert.getAcknowledgedAt());

        service.evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("6.50"));
        assertSame(originalAcknowledgement, alert.getAcknowledgedAt());
        assertEquals(AlertSeverity.WARNING, alert.getSeverity());
    }

    @Test
    void resolutionPreservesAcknowledgementAndLaterLowGradeCreatesNewPendingEpisode() {
        Alert resolvedEpisode = alert(AlertSeverity.WARNING, "6.50");
        resolvedEpisode.acknowledge();
        var originalAcknowledgement = resolvedEpisode.getAcknowledgedAt();
        when(alertRepository.findByActivityIdAndStudentIdAndRuleCodeAndStatus(
                        ACTIVITY_ID, STUDENT_ID, "LOW_GRADE", AlertStatus.OPEN))
                .thenReturn(Optional.of(resolvedEpisode), Optional.empty());
        AlertEvaluationService service = new AlertEvaluationService(alertRepository);

        service.evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("8.00"));

        assertEquals(AlertStatus.RESOLVED, resolvedEpisode.getStatus());
        assertSame(originalAcknowledgement, resolvedEpisode.getAcknowledgedAt());

        service.evaluate(INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, new BigDecimal("5.00"));

        ArgumentCaptor<Alert> savedAlerts = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, org.mockito.Mockito.times(2)).save(savedAlerts.capture());
        Alert newEpisode = savedAlerts.getAllValues().get(1);
        assertNotSame(resolvedEpisode, newEpisode);
        assertTrue(newEpisode.isOpen());
        assertTrue(newEpisode.isPending());
        assertFalse(newEpisode.isAcknowledged());
        assertEquals(AlertSeverity.CRITICAL, newEpisode.getSeverity());
    }

    private static Alert alert(AlertSeverity severity, String score) {
        return new Alert(
                INSTITUTION_ID, COURSE_ID, ACTIVITY_ID, STUDENT_ID, "LOW_GRADE", severity, new BigDecimal(score));
    }
}
