package io.academicmonitor.communication.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.academicmonitor.academic.domain.*;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.domain.*;
import io.academicmonitor.monitoring.domain.AlertRepository;
import io.academicmonitor.notification.application.AppNotificationService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class DeleteDraftServiceTest {
    private final UUID institution = UUID.randomUUID();
    private final UUID teacher = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private final CommunicationRepository repository = mock(CommunicationRepository.class);
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final CommunicationDeliveryPort delivery = mock(CommunicationDeliveryPort.class);
    private final AppNotificationService notifications = mock(AppNotificationService.class);
    private AlertCommunicationService service;
    private Communication draft;

    @BeforeEach
    void setUp() {
        draft = Communication.draft(
                institution,
                teacher,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "IDUKAY",
                "Subject",
                "Content");
        when(repository.findById(id)).thenReturn(Optional.of(draft));
        service = new AlertCommunicationService(
                alerts,
                mock(AcademicCourseRepository.class),
                mock(ActivityRepository.class),
                mock(GradeRepository.class),
                mock(StudentRepository.class),
                mock(StudentGuardianRepository.class),
                mock(GuardianRepository.class),
                repository,
                new TemplateMessageGenerator(),
                delivery,
                new CommunicationWorkflowStore(repository, notifications));
    }

    @Test
    void deletesDraftWithoutTouchingAlertDeliveryOrNotifications() {
        service.deleteDraft(institution, teacher, id);
        verify(repository).delete(draft);
        verifyNoInteractions(alerts, delivery, notifications);
    }

    @ParameterizedTest
    @EnumSource(
            value = CommunicationStatus.class,
            names = {"PENDING", "SENT", "FAILED"})
    void rejectsNonDraftWithoutChangingIt(CommunicationStatus status) {
        draft.beginSending();
        if (status == CommunicationStatus.SENT) draft.markSent(Instant.now());
        if (status == CommunicationStatus.FAILED) draft.markFailed("FAILED", "Failure");
        assertFalse(draft.isDeletable());
        var error =
                assertThrows(CommunicationWorkflowException.class, () -> service.deleteDraft(institution, teacher, id));
        assertEquals(CommunicationWorkflowError.COMMUNICATION_NOT_DELETABLE, error.getError());
        assertEquals(status, draft.getStatus());
        verify(repository, never()).delete(any());
        verifyNoInteractions(alerts, delivery, notifications);
    }

    @Test
    void translatesStaleDeleteToDomainConflict() {
        doThrow(new ObjectOptimisticLockingFailureException(Communication.class, id))
                .when(repository)
                .delete(draft);
        var error =
                assertThrows(CommunicationWorkflowException.class, () -> service.deleteDraft(institution, teacher, id));
        assertEquals(CommunicationWorkflowError.COMMUNICATION_NOT_DELETABLE, error.getError());
        verifyNoInteractions(alerts, delivery, notifications);
    }
}
