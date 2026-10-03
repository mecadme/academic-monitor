package io.academicmonitor.communication.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.academicmonitor.academic.domain.*;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.communication.domain.CommunicationRepository;
import io.academicmonitor.communication.domain.CommunicationStatus;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertCommunicationServiceTest {
    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ALERT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COURSE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ACTIVITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID STUDENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID GUARDIAN_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Test
    void prepareCreatesDraftFromAcademicDataWithoutCallingDelivery() {
        AlertRepository alerts = mock(AlertRepository.class);
        AcademicCourseRepository courses = mock(AcademicCourseRepository.class);
        ActivityRepository activities = mock(ActivityRepository.class);
        GradeRepository grades = mock(GradeRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        StudentGuardianRepository relationships = mock(StudentGuardianRepository.class);
        GuardianRepository guardians = mock(GuardianRepository.class);
        CommunicationRepository communications = mock(CommunicationRepository.class);
        CommunicationDeliveryPort delivery = mock(CommunicationDeliveryPort.class);
        CommunicationWorkflowStore store = mock(CommunicationWorkflowStore.class);

        Alert alert = mock(Alert.class);
        when(alert.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(alert.getCourseId()).thenReturn(COURSE_ID);
        when(alert.getActivityId()).thenReturn(ACTIVITY_ID);
        when(alert.getStudentId()).thenReturn(STUDENT_ID);
        when(alert.getId()).thenReturn(ALERT_ID);
        when(alert.isOpen()).thenReturn(true);
        when(alert.getScoreSnapshot()).thenReturn(new BigDecimal("4.50"));
        when(alerts.findById(ALERT_ID)).thenReturn(Optional.of(alert));

        AcademicCourse course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(COURSE_ID);
        when(course.getSubject()).thenReturn("Matemática");
        when(courses.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_ID))
                .thenReturn(List.of(course));
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(STUDENT_ID);
        when(student.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(students.findStudentById(STUDENT_ID)).thenReturn(Optional.of(student));
        StudentGuardian relationship = mock(StudentGuardian.class);
        when(relationship.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(relationship.getGuardianId()).thenReturn(GUARDIAN_ID);
        when(relationship.isOfficialLegalGuardian()).thenReturn(true);
        when(relationships.findByStudentId(STUDENT_ID)).thenReturn(List.of(relationship));
        Guardian guardian = mock(Guardian.class);
        when(guardian.getId()).thenReturn(GUARDIAN_ID);
        when(guardian.hasSystemAccess()).thenReturn(true);
        when(guardian.getExternalUserId()).thenReturn("provider-recipient");
        when(guardians.findByInstitutionIdAndIdIn(eq(INSTITUTION_ID), anySet())).thenReturn(List.of(guardian));
        Activity activity = mock(Activity.class);
        when(activity.getId()).thenReturn(ACTIVITY_ID);
        when(activity.getName()).thenReturn("Evaluación 1");
        when(activity.getMaxScore()).thenReturn(new BigDecimal("10"));
        when(activities.findActivitiesByCourseIdIn(anySet())).thenReturn(List.of(activity));
        when(communications.findByAlertId(ALERT_ID)).thenReturn(List.of());
        when(communications.save(any(Communication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(delivery.providerCode()).thenReturn("IDUKAY");

        AlertCommunicationService service = new AlertCommunicationService(
                alerts,
                courses,
                activities,
                grades,
                students,
                relationships,
                guardians,
                communications,
                new TemplateMessageGenerator(),
                delivery,
                store);

        CommunicationResponse response = service.prepare(INSTITUTION_ID, TEACHER_ID, ALERT_ID);

        assertEquals(CommunicationStatus.DRAFT, response.status());
        assertEquals("Seguimiento académico - Matemática", response.subject());
        assertEquals(true, response.content().contains("4.50/10"));
        verify(communications).save(any(Communication.class));
        verifyNoInteractions(store);
        verify(delivery, never()).send(any());
    }
}
