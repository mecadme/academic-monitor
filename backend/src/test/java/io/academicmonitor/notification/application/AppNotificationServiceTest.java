package io.academicmonitor.notification.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.notification.domain.AppNotification;
import io.academicmonitor.notification.domain.AppNotificationRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class AppNotificationServiceTest {
    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ALERT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private AppNotificationRepository repository;
    private AppNotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppNotificationRepository.class);
        service = new AppNotificationService(repository);
    }

    @Test
    void newCriticalAlertCreatesAnOwnedDeduplicatedNotification() {
        service.newCriticalAlert(INSTITUTION_ID, TEACHER_ID, ALERT_ID, null, "Ana Pérez", "Física");

        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
        verify(repository).save(captor.capture());
        AppNotification notification = captor.getValue();
        assertEquals(INSTITUTION_ID, notification.getInstitutionId());
        assertEquals(TEACHER_ID, notification.getTeacherUserId());
        assertEquals("Nueva alerta crítica", notification.getTitle());
        assertEquals("Ana Pérez requiere atención en Física.", notification.getMessage());
        assertEquals(ALERT_ID, notification.getReferenceId());
    }

    @Test
    void existingEventKeyPreventsDuplicateCriticalAlertNotification() {
        when(repository.existsByOwnerAndEventKey(INSTITUTION_ID, TEACHER_ID, "NEW_CRITICAL_ALERT:" + ALERT_ID))
                .thenReturn(true);

        service.newCriticalAlert(INSTITUTION_ID, TEACHER_ID, ALERT_ID, null, "Ana Pérez", "Física");

        verify(repository, never()).save(any());
    }

    @Test
    void listAndMarkAllReadAreScopedToCurrentTeacher() {
        when(repository.findByOwner(INSTITUTION_ID, TEACHER_ID, false, 20)).thenReturn(List.of());
        when(repository.countUnread(INSTITUTION_ID, TEACHER_ID)).thenReturn(3L);

        assertEquals(3, service.list(INSTITUTION_ID, TEACHER_ID, false, null).unreadCount());
        service.markAllRead(INSTITUTION_ID, TEACHER_ID);

        verify(repository).findByOwner(INSTITUTION_ID, TEACHER_ID, false, 20);
        verify(repository)
                .markAllRead(
                        org.mockito.ArgumentMatchers.eq(INSTITUTION_ID),
                        org.mockito.ArgumentMatchers.eq(TEACHER_ID),
                        any());
    }

    @Test
    void markReadIsIdempotentAndDoesNotCrossTenantBoundaries() throws Exception {
        AppNotification notification = new AppNotification(
                INSTITUTION_ID,
                TEACHER_ID,
                io.academicmonitor.notification.domain.AppNotificationType.SYNC_COMPLETED,
                "Sincronización completada",
                "1 cursos sincronizados · 2 calificaciones procesadas.",
                io.academicmonitor.notification.domain.AppNotificationReferenceType.SYNC,
                null,
                null,
                null);
        setField(notification, "id", ALERT_ID);
        setField(notification, "createdAt", Instant.now());
        when(repository.findByOwnerAndId(INSTITUTION_ID, TEACHER_ID, ALERT_ID)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        service.markRead(INSTITUTION_ID, TEACHER_ID, ALERT_ID);
        Instant firstReadAt = notification.getReadAt();
        service.markRead(INSTITUTION_ID, TEACHER_ID, ALERT_ID);

        assertEquals(firstReadAt, notification.getReadAt());
        when(repository.findByOwnerAndId(INSTITUTION_ID, UUID.randomUUID(), ALERT_ID))
                .thenReturn(Optional.empty());
        assertThrows(
                AppNotificationNotFoundException.class,
                () -> service.markRead(INSTITUTION_ID, UUID.randomUUID(), ALERT_ID));
    }

    @Test
    void secondaryNotificationFailureDoesNotChangeSuccessfulDeliveryTruth() {
        when(repository.existsByOwnerAndEventKey(INSTITUTION_ID, TEACHER_ID, "COMMUNICATION_SENT:" + ALERT_ID))
                .thenReturn(false);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("storage unavailable"));

        assertDoesNotThrow(() -> service.communicationSent(INSTITUTION_ID, TEACHER_ID, ALERT_ID));
    }

    private static void setField(AppNotification notification, String name, Object value) throws Exception {
        Field field = AppNotification.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(notification, value);
    }
}
