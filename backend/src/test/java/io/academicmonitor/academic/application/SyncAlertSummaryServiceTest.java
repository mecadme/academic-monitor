package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.monitoring.domain.AlertStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncAlertSummaryServiceTest {

    private static final UUID COURSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ACTIVITY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private Alert warning;

    @Mock
    private Alert critical;

    @Test
    void summarizesOpenAlertsOnlyForProcessedActivities() {

        when(warning.getSeverity()).thenReturn(AlertSeverity.WARNING);
        when(critical.getSeverity()).thenReturn(AlertSeverity.CRITICAL);

        when(alertRepository.findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID)))
                .thenReturn(List.of(warning, warning, critical));

        SyncAlertSummaryService.Summary result =
                new SyncAlertSummaryService(alertRepository).summarize(COURSE_ID, List.of(ACTIVITY_ID));

        assertEquals(3, result.openAlerts());
        assertEquals(2, result.warnings());
        assertEquals(1, result.critical());

        verify(alertRepository)
                .findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of(ACTIVITY_ID));
    }

    @Test
    void avoidsUnboundedAlertQueryWhenNoActivitiesWereProcessed() {

        SyncAlertSummaryService.Summary result =
                new SyncAlertSummaryService(alertRepository).summarize(COURSE_ID, List.of());

        assertEquals(0, result.openAlerts());
        assertEquals(0, result.warnings());
        assertEquals(0, result.critical());

        verify(alertRepository, never()).findByCourseIdAndStatusAndActivityIdIn(COURSE_ID, AlertStatus.OPEN, List.of());
    }
}
