package io.academicmonitor.communication.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.academicmonitor.academic.domain.*;
import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.application.TemplateMessageGenerator;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.domain.*;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import io.academicmonitor.monitoring.domain.AlertRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DeleteCommunicationControllerTest {
    private final UUID institutionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID communicationId = UUID.randomUUID();
    private final CommunicationRepository repository = mock(CommunicationRepository.class);
    private final AuthenticatedAcademicContext context = mock(AuthenticatedAcademicContext.class);
    private final CommunicationDeliveryPort delivery = mock(CommunicationDeliveryPort.class);
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final AcademicCourseRepository courses = mock(AcademicCourseRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final GradeRepository grades = mock(GradeRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final StudentGuardianRepository relationships = mock(StudentGuardianRepository.class);
    private final GuardianRepository guardians = mock(GuardianRepository.class);
    private final Map<UUID, Communication> rows = new HashMap<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(context.institutionId()).thenReturn(institutionId);
        when(context.userId()).thenReturn(teacherId);
        when(repository.findById(communicationId))
                .thenAnswer(ignored -> Optional.ofNullable(rows.get(communicationId)));
        doAnswer(invocation -> {
                    rows.remove(communicationId);
                    return null;
                })
                .when(repository)
                .delete(any());
        var service = new AlertCommunicationService(
                alerts,
                courses,
                activities,
                grades,
                students,
                relationships,
                guardians,
                repository,
                new TemplateMessageGenerator(),
                delivery,
                null);
        mvc = MockMvcBuilders.standaloneSetup(new CommunicationController(service, context))
                .setControllerAdvice(new CommunicationExceptionHandler())
                .build();
    }

    @Test
    void draftIsPhysicallyDeletedWithoutDeliveryOrAcademicChangesAndRepeatedDeleteIsNotFound() throws Exception {
        rows.put(communicationId, draft(institutionId, teacherId));
        mvc.perform(delete(path()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertFalse(rows.containsKey(communicationId));
        verifyNoInteractions(delivery, alerts, courses, activities, grades, students, relationships, guardians);
        mvc.perform(delete(path()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNICATION_NOT_FOUND"));
    }

    @Test
    void cannotDeleteAnotherTeachersDraftEvenWithSpoofedScopeParameters() throws Exception {
        UUID otherTeacher = UUID.randomUUID();
        rows.put(communicationId, draft(institutionId, otherTeacher));
        mvc.perform(delete(path())
                        .queryParam("teacherUserId", otherTeacher.toString())
                        .queryParam("institutionId", institutionId.toString()))
                .andExpect(status().isNotFound());
        verify(repository, never()).delete(any());
        assertTrue(rows.containsKey(communicationId));
    }

    @Test
    void cannotDeleteAnotherInstitutionsDraft() throws Exception {
        rows.put(communicationId, draft(UUID.randomUUID(), teacherId));
        mvc.perform(delete(path())).andExpect(status().isNotFound());
        verify(repository, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = CommunicationStatus.class,
            names = {"PENDING", "SENT", "FAILED"})
    void nonDraftReturnsCleanConflict(CommunicationStatus status) throws Exception {
        Communication communication = draft(institutionId, teacherId);
        communication.beginSending();
        if (status == CommunicationStatus.SENT) communication.markSent(Instant.now());
        if (status == CommunicationStatus.FAILED) communication.markFailed("FAILED", "Failure");
        rows.put(communicationId, communication);
        mvc.perform(delete(path()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMUNICATION_NOT_DELETABLE"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
        verify(repository, never()).delete(any());
        assertEquals(status, communication.getStatus());
    }

    @Test
    void nonexistentIsNotFound() throws Exception {
        mvc.perform(delete(path())).andExpect(status().isNotFound());
        verify(repository, never()).delete(any());
    }

    @Test
    void staleDraftReturnsDomainConflictWithoutTechnicalDetails() throws Exception {
        rows.put(communicationId, draft(institutionId, teacherId));
        doThrow(new ObjectOptimisticLockingFailureException(Communication.class, communicationId))
                .when(repository)
                .delete(any());
        mvc.perform(delete(path()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMUNICATION_NOT_DELETABLE"))
                .andExpect(jsonPath("$.detail").value("The requested communication action cannot be completed."));
        assertTrue(rows.containsKey(communicationId));
        verifyNoInteractions(delivery);
    }

    @Test
    void unavailableServerContextFailsClosedWithoutReadingCommunications() throws Exception {
        when(context.institutionId())
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED));
        mvc.perform(delete(path())).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository, delivery);
    }

    private String path() {
        return "/api/v1/communications/" + communicationId;
    }

    private Communication draft(UUID institution, UUID teacher) {
        return Communication.draft(
                institution,
                teacher,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "IDUKAY",
                "Subject",
                "Content");
    }
}
