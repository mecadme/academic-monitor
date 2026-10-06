package io.academicmonitor.communication.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.academicmonitor.academic.domain.*;
import io.academicmonitor.communication.api.CommunicationController;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.domain.*;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.domain.*;
import io.academicmonitor.notification.domain.AppNotificationRepository;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DeleteDraftWorkflowIT extends PostgresIntegrationTest {
    @Autowired
    private AlertCommunicationService service;

    @Autowired
    private CommunicationRepository communications;

    @Autowired
    private AlertInboxQueryService inbox;

    @Autowired
    private AppNotificationRepository notifications;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CommunicationDeliveryPort delivery;

    private UUID institutionId;
    private UUID teacherId;
    private Alert alert;
    private Activity activity;
    private Student student;
    private CommunicationResponse draft;

    @BeforeEach
    void setUp() {
        institutionId =
                persist(new Institution("Draft workflow", "America/Guayaquil")).getId();
        teacherId = persist(new User("draft-" + UUID.randomUUID() + "@example.test"))
                .getId();
        persist(new InstitutionMembership(teacherId, institutionId, InstitutionRole.TEACHER));
        String suffix = UUID.randomUUID().toString();
        AcademicYear year =
                persist(new AcademicYear(institutionId, "TEST", suffix, "Year", "2026-2027", BigDecimal.TEN));
        AcademicPeriod period = persist(new AcademicPeriod(year.getId(), suffix, "Period", "P1", 1));
        AcademicCourse course = persist(
                new AcademicCourse(institutionId, teacherId, year.getId(), "TEST", suffix, "Course", "Physics"));
        student = persist(new Student(institutionId, "TEST", suffix, "Student", "Name"));
        Guardian guardian = persist(new Guardian(institutionId, "TEST", suffix, "recipient", "Guardian", null, true));
        persist(new StudentGuardian(
                institutionId, student.getId(), guardian.getId(), "Parent", true, true, false, false, false));
        activity = persist(new Activity(
                course.getId(), period.getId(), "TEST", suffix, "Activity", BigDecimal.TEN, LocalDate.of(2026, 10, 1)));
        alert = persist(new Alert(
                institutionId,
                course.getId(),
                activity.getId(),
                student.getId(),
                "LOW_GRADE",
                AlertSeverity.CRITICAL,
                new BigDecimal("4.00")));
        when(delivery.providerCode()).thenReturn("IDUKAY");
        draft = service.prepare(institutionId, teacherId, alert.getId());
        clearInvocations(delivery);
    }

    @Test
    void deletingThroughApiRemovesPersistedDraftRestoresInboxAndAllowsPreparingAgainWithoutSideEffects()
            throws Exception {
        assertEquals(
                draft.id(),
                inbox.getInbox(institutionId, teacherId, null)
                        .alerts()
                        .getFirst()
                        .communication()
                        .id());
        long notificationCount =
                notifications.findByOwner(institutionId, teacherId, false, 100).size();
        Object[] alertBefore = alertRow();
        var authenticatedContext = mock(AuthenticatedAcademicContext.class);
        when(authenticatedContext.institutionId()).thenReturn(institutionId);
        when(authenticatedContext.userId()).thenReturn(teacherId);
        var mvc = MockMvcBuilders.standaloneSetup(new CommunicationController(service, authenticatedContext))
                .build();
        mvc.perform(delete("/api/v1/communications/" + draft.id())).andExpect(status().isNoContent());
        entityManager.clear();

        assertTrue(communications.findById(draft.id()).isEmpty());
        assertTrue(communications.findByAlertId(alert.getId()).isEmpty());
        assertTrue(
                communications.findByAlertIdIn(java.util.Set.of(alert.getId())).isEmpty());
        assertNull(inbox.getInbox(institutionId, teacherId, null)
                .alerts()
                .getFirst()
                .communication());
        Alert unchanged = entityManager.find(Alert.class, alert.getId());
        assertEquals(AlertStatus.OPEN, unchanged.getStatus());
        assertNull(unchanged.getAcknowledgedAt());
        assertArrayEquals(alertBefore, alertRow());
        assertEquals(new BigDecimal("4.00"), unchanged.getScoreSnapshot());
        assertEquals(
                BigDecimal.TEN.setScale(2),
                entityManager.find(Activity.class, activity.getId()).getMaxScore());
        assertEquals(
                student.getFullName(),
                entityManager.find(Student.class, student.getId()).getFullName());
        assertEquals(
                notificationCount,
                notifications.findByOwner(institutionId, teacherId, false, 100).size());
        verifyNoInteractions(delivery);

        CommunicationResponse replacement = service.prepare(institutionId, teacherId, alert.getId());
        assertEquals(CommunicationStatus.DRAFT, replacement.status());
        assertNotEquals(draft.id(), replacement.id());
        assertEquals(
                replacement.id(),
                inbox.getInbox(institutionId, teacherId, null)
                        .alerts()
                        .getFirst()
                        .communication()
                        .id());
        verify(delivery, never()).send(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = CommunicationStatus.class,
            names = {"PENDING", "SENT", "FAILED"})
    void staleLoadedDraftCannotBeDeletedAfterSendingUpdatesVersion(CommunicationStatus status) {
        // Keep the DRAFT managed with its old version, then simulate the sending transaction's SQL update.
        Communication stale = communications.findById(draft.id()).orElseThrow();
        String completion = status == CommunicationStatus.SENT
                ? ", sent_at = CURRENT_TIMESTAMP"
                : status == CommunicationStatus.FAILED ? ", failure_code = 'FAILED', failure_reason = 'Failure'" : "";
        entityManager
                .createNativeQuery("UPDATE communications SET status = :status, version = version + 1" + completion
                        + " WHERE id = :id")
                .setParameter("status", status.name())
                .setParameter("id", draft.id())
                .executeUpdate();
        assertTrue(stale.isDeletable());
        var error = assertThrows(
                CommunicationWorkflowException.class, () -> service.deleteDraft(institutionId, teacherId, draft.id()));
        assertEquals(CommunicationWorkflowError.COMMUNICATION_NOT_DELETABLE, error.getError());
        verifyNoInteractions(delivery);
    }

    @Test
    void sendingAStaleDraftAfterDeleteCannotClaimOrDeliverIt() {
        Communication stale = communications.findById(draft.id()).orElseThrow();
        entityManager.detach(stale);
        service.deleteDraft(institutionId, teacherId, draft.id());
        stale.beginSending();
        assertThrows(OptimisticLockingFailureException.class, () -> communications.save(stale));
        verifyNoInteractions(delivery);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    private Object[] alertRow() {
        return (Object[]) entityManager
                .createNativeQuery("SELECT * FROM alerts WHERE id = :id")
                .setParameter("id", alert.getId())
                .getSingleResult();
    }
}
