package io.academicmonitor.notification.application;

import io.academicmonitor.notification.domain.AppNotification;
import io.academicmonitor.notification.domain.AppNotificationReferenceType;
import io.academicmonitor.notification.domain.AppNotificationRepository;
import io.academicmonitor.notification.domain.AppNotificationType;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppNotificationService.class);
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final AppNotificationRepository repository;

    public AppNotificationService(AppNotificationRepository repository) {
        this.repository = repository;
    }

    /** Internal alert notifications share the academic transaction. */
    public void newCriticalAlert(
            UUID institutionId,
            UUID teacherUserId,
            UUID alertId,
            UUID academicPeriodId,
            String studentName,
            String subject) {
        String displaySubject = subject == null || subject.isBlank() ? "este curso" : subject.trim();
        String message = studentName == null || studentName.isBlank()
                ? "Un estudiante requiere atención en " + displaySubject + "."
                : studentName.trim() + " requiere atención en " + displaySubject + ".";
        createDeduplicated(
                institutionId,
                teacherUserId,
                AppNotificationType.NEW_CRITICAL_ALERT,
                "Nueva alerta crítica",
                message,
                AppNotificationReferenceType.ALERT,
                alertId,
                academicPeriodId,
                "NEW_CRITICAL_ALERT:" + alertId);
    }

    /** Delivery has already happened: notification persistence must never change that truth. */
    public void communicationSent(UUID institutionId, UUID teacherUserId, UUID communicationId) {
        recordSecondary(
                institutionId,
                teacherUserId,
                AppNotificationType.COMMUNICATION_SENT,
                "Comunicación enviada",
                "La comunicación fue enviada correctamente por Idukay.",
                AppNotificationReferenceType.COMMUNICATION,
                communicationId,
                null,
                "COMMUNICATION_SENT:" + communicationId);
    }

    public void communicationFailed(UUID institutionId, UUID teacherUserId, UUID communicationId) {
        recordSecondary(
                institutionId,
                teacherUserId,
                AppNotificationType.COMMUNICATION_FAILED,
                "No se pudo enviar la comunicación",
                "Revisa la comunicación e intenta nuevamente cuando corresponda.",
                AppNotificationReferenceType.COMMUNICATION,
                communicationId,
                null,
                "COMMUNICATION_FAILED:" + communicationId);
    }

    public void syncCompleted(
            UUID institutionId, UUID teacherUserId, UUID academicPeriodId, int coursesProcessed, int gradesProcessed) {
        recordSecondary(
                institutionId,
                teacherUserId,
                AppNotificationType.SYNC_COMPLETED,
                "Sincronización completada",
                coursesProcessed + " cursos sincronizados · " + gradesProcessed + " calificaciones procesadas.",
                AppNotificationReferenceType.SYNC,
                null,
                academicPeriodId,
                null);
    }

    public void syncFailed(UUID institutionId, UUID teacherUserId, UUID academicPeriodId) {
        recordSecondary(
                institutionId,
                teacherUserId,
                AppNotificationType.SYNC_FAILED,
                "Sincronización incompleta",
                "No se pudo actualizar el período seleccionado.",
                AppNotificationReferenceType.SYNC,
                null,
                academicPeriodId,
                null);
    }

    @Transactional(readOnly = true)
    public AppNotificationListResponse list(UUID institutionId, UUID teacherUserId, boolean unreadOnly, Integer limit) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        return new AppNotificationListResponse(
                repository.findByOwner(institutionId, teacherUserId, unreadOnly, effectiveLimit).stream()
                        .map(AppNotificationResponse::from)
                        .toList(),
                repository.countUnread(institutionId, teacherUserId));
    }

    @Transactional
    public AppNotificationResponse markRead(UUID institutionId, UUID teacherUserId, UUID notificationId) {
        AppNotification notification = repository
                .findByOwnerAndId(institutionId, teacherUserId, notificationId)
                .orElseThrow(() -> new AppNotificationNotFoundException(notificationId));
        notification.markRead(Instant.now());
        return AppNotificationResponse.from(repository.save(notification));
    }

    @Transactional
    public void markAllRead(UUID institutionId, UUID teacherUserId) {
        repository.markAllRead(institutionId, teacherUserId, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecondary(
            UUID institutionId,
            UUID teacherUserId,
            AppNotificationType type,
            String title,
            String message,
            AppNotificationReferenceType referenceType,
            UUID referenceId,
            UUID academicPeriodId,
            String eventKey) {
        try {
            createDeduplicated(
                    institutionId,
                    teacherUserId,
                    type,
                    title,
                    message,
                    referenceType,
                    referenceId,
                    academicPeriodId,
                    eventKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist app notification id={} type={} operation=secondary", referenceId, type);
        }
    }

    private void createDeduplicated(
            UUID institutionId,
            UUID teacherUserId,
            AppNotificationType type,
            String title,
            String message,
            AppNotificationReferenceType referenceType,
            UUID referenceId,
            UUID academicPeriodId,
            String eventKey) {
        if (eventKey != null && repository.existsByOwnerAndEventKey(institutionId, teacherUserId, eventKey)) return;
        try {
            repository.save(new AppNotification(
                    institutionId,
                    teacherUserId,
                    type,
                    title,
                    message,
                    referenceType,
                    referenceId,
                    academicPeriodId,
                    eventKey));
        } catch (DataIntegrityViolationException exception) {
            if (eventKey == null) throw exception;
            LOGGER.debug("App notification already recorded type={} operation=deduplicate", type);
        }
    }
}
