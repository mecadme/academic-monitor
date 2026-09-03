package io.academicmonitor.monitoring.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertTriageServiceTest {

    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID OTHER_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID OTHER_COURSE_ID = UUID.randomUUID();

    private AcademicCourseRepository courseRepository;
    private AlertRepository alertRepository;
    private AlertTriageService service;

    @BeforeEach
    void setUp() {
        courseRepository = mock(AcademicCourseRepository.class);
        alertRepository = mock(AlertRepository.class);
        service = new AlertTriageService(courseRepository, alertRepository);
    }

    @Test
    void acknowledgesAndMarksPendingAnOwnedOpenAlert() {
        Alert alert = alert(INSTITUTION_ID, COURSE_ID);
        allowCourse(COURSE_ID);
        when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

        service.acknowledge(INSTITUTION_ID, TEACHER_ID, ALERT_ID);

        assertTrue(alert.isAcknowledged());
        verify(alertRepository).save(alert);

        service.markPending(INSTITUTION_ID, TEACHER_ID, ALERT_ID);

        assertTrue(alert.isPending());
        verify(alertRepository, org.mockito.Mockito.times(2)).save(alert);
    }

    @Test
    void openIdempotentOperationDoesNotIssueAnUnnecessarySave() {
        Alert alert = alert(INSTITUTION_ID, COURSE_ID);
        alert.acknowledge();
        allowCourse(COURSE_ID);
        when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

        service.acknowledge(INSTITUTION_ID, TEACHER_ID, ALERT_ID);

        verify(alertRepository, never()).save(alert);
    }

    @Test
    void foreignInstitutionAndAnotherTeacherReceiveTheSameNotFoundBehavior() {
        allowCourse(COURSE_ID);
        when(alertRepository.findById(ALERT_ID))
                .thenReturn(Optional.of(alert(OTHER_INSTITUTION_ID, COURSE_ID)))
                .thenReturn(Optional.of(alert(INSTITUTION_ID, OTHER_COURSE_ID)));

        assertThrows(
                AlertTriageNotFoundException.class, () -> service.acknowledge(INSTITUTION_ID, TEACHER_ID, ALERT_ID));
        assertThrows(
                AlertTriageNotFoundException.class, () -> service.acknowledge(INSTITUTION_ID, TEACHER_ID, ALERT_ID));
        verify(alertRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noScopedCoursesDoesNotLookUpTheAlertAndCannotLeakItsExistence() {
        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_ID))
                .thenReturn(List.of());

        assertThrows(
                AlertTriageNotFoundException.class, () -> service.acknowledge(INSTITUTION_ID, TEACHER_ID, ALERT_ID));

        verifyNoInteractions(alertRepository);
    }

    @Test
    void resolvedAlertCannotBeAcknowledgedOrMarkedPending() {
        Alert alert = alert(INSTITUTION_ID, COURSE_ID);
        alert.acknowledge();
        alert.resolve();
        allowCourse(COURSE_ID);
        when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

        assertThrows(
                AlertTriageConflictException.class, () -> service.acknowledge(INSTITUTION_ID, TEACHER_ID, ALERT_ID));
        assertThrows(
                AlertTriageConflictException.class, () -> service.markPending(INSTITUTION_ID, TEACHER_ID, ALERT_ID));
        assertTrue(alert.isAcknowledged());
        verify(alertRepository, never()).save(alert);
    }

    private void allowCourse(UUID courseId) {
        AcademicCourse course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(courseId);
        when(courseRepository.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_ID))
                .thenReturn(List.of(course));
    }

    private static Alert alert(UUID institutionId, UUID courseId) {
        return new Alert(
                institutionId,
                courseId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "LOW_GRADE",
                AlertSeverity.WARNING,
                new BigDecimal("6.50"));
    }
}
