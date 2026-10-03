package io.academicmonitor.communication.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.academicmonitor.academic.domain.*;
import io.academicmonitor.communication.application.port.CommunicationDeliveryPort;
import io.academicmonitor.communication.application.port.CommunicationDeliveryRequest;
import io.academicmonitor.communication.application.port.DeliveryResult;
import io.academicmonitor.communication.domain.Communication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ManualNotificationServiceTest {

    private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEACHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COURSE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GUARDIAN_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID COMMUNICATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private AcademicCourseRepository courses;
    private CourseEnrollmentRepository enrollments;
    private StudentRepository students;
    private StudentGuardianRepository relationships;
    private GuardianRepository guardians;
    private CommunicationAuditService audit;
    private CommunicationDeliveryPort delivery;
    private ManualNotificationService service;

    @BeforeEach
    void setUp() {
        courses = mock(AcademicCourseRepository.class);
        enrollments = mock(CourseEnrollmentRepository.class);
        students = mock(StudentRepository.class);
        relationships = mock(StudentGuardianRepository.class);
        guardians = mock(GuardianRepository.class);
        audit = mock(CommunicationAuditService.class);
        delivery = mock(CommunicationDeliveryPort.class);
        service = new ManualNotificationService(
                courses, enrollments, students, relationships, guardians, audit, delivery);
    }

    @Test
    void sendsToTheOnlyOfficialLegalGuardianExternalUserId() {
        Guardian guardian = reachableGuardian("guardian-idukay-user");
        arrangeReachableSingleOfficialGuardian(guardian);
        Communication communication = mock(Communication.class);
        when(communication.getId()).thenReturn(COMMUNICATION_ID);
        when(delivery.providerCode()).thenReturn("IDUKAY");
        when(audit.createPending(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(communication);
        when(delivery.send(any())).thenReturn(DeliveryResult.sent());

        ManualNotificationResult result = service.send(request());

        ArgumentCaptor<CommunicationDeliveryRequest> requestCaptor =
                ArgumentCaptor.forClass(CommunicationDeliveryRequest.class);
        verify(delivery).send(requestCaptor.capture());
        assertEquals("guardian-idukay-user", requestCaptor.getValue().recipientExternalUserId());
        assertEquals("Subject", requestCaptor.getValue().subject());
        assertEquals("<p>Content</p>", requestCaptor.getValue().content());
        assertEquals(COMMUNICATION_ID, result.communicationId());
        assertEquals(null, result.failure());
        verify(audit).markSent(COMMUNICATION_ID);
    }

    @Test
    void doesNotSendWhenNoOfficialLegalGuardianExists() {
        arrangeStudentInTeacherScope();
        StudentGuardian relationship = mock(StudentGuardian.class);
        when(relationship.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(relationship.isOfficialLegalGuardian()).thenReturn(false);
        when(relationships.findByStudentId(STUDENT_ID)).thenReturn(List.of(relationship));

        ManualNotificationResult result = service.send(request());

        assertEquals(ManualNotificationFailure.RECIPIENT_NOT_FOUND, result.failure());
        verifyNoInteractions(audit, delivery, guardians);
    }

    @Test
    void doesNotSendWhenMoreThanOneOfficialLegalGuardianExists() {
        arrangeStudentInTeacherScope();
        StudentGuardian first = officialRelationship(GUARDIAN_ID);
        StudentGuardian second = officialRelationship(UUID.randomUUID());
        when(relationships.findByStudentId(STUDENT_ID)).thenReturn(List.of(first, second));

        ManualNotificationResult result = service.send(request());

        assertEquals(ManualNotificationFailure.RECIPIENT_AMBIGUOUS, result.failure());
        verifyNoInteractions(audit, delivery, guardians);
    }

    @Test
    void doesNotSendWhenGuardianHasNoExternalUserId() {
        Guardian guardian = reachableGuardian(null);
        arrangeReachableSingleOfficialGuardian(guardian);

        ManualNotificationResult result = service.send(request());

        assertEquals(ManualNotificationFailure.RECIPIENT_NOT_REACHABLE, result.failure());
        verifyNoInteractions(audit, delivery);
    }

    @Test
    void doesNotSendWhenGuardianSystemAccessIsFalse() {
        Guardian guardian = reachableGuardian("guardian-idukay-user");
        when(guardian.hasSystemAccess()).thenReturn(false);
        arrangeReachableSingleOfficialGuardian(guardian);

        ManualNotificationResult result = service.send(request());

        assertEquals(ManualNotificationFailure.RECIPIENT_NOT_REACHABLE, result.failure());
        verifyNoInteractions(audit, delivery);
    }

    @Test
    void doesNotSendWhenStudentIsOutsideInstitutionOrTeacherScope() {
        Student otherInstitutionStudent = mock(Student.class);
        when(otherInstitutionStudent.getInstitutionId()).thenReturn(UUID.randomUUID());
        when(students.findStudentById(STUDENT_ID)).thenReturn(Optional.of(otherInstitutionStudent));

        ManualNotificationResult result = service.send(request());

        assertEquals(ManualNotificationFailure.STUDENT_OUT_OF_SCOPE, result.failure());
        verifyNoInteractions(relationships, guardians, audit, delivery);
    }

    @Test
    void recordsSenderResolutionFailureWithoutCallingDeliveryAgain() {
        Guardian guardian = reachableGuardian("guardian-idukay-user");
        arrangeReachableSingleOfficialGuardian(guardian);
        Communication communication = mock(Communication.class);
        when(communication.getId()).thenReturn(COMMUNICATION_ID);
        when(delivery.providerCode()).thenReturn("IDUKAY");
        when(audit.createPending(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(communication);
        when(delivery.send(any())).thenReturn(DeliveryResult.failed("SENDER_NOT_RESOLVED", "sender missing"));

        ManualNotificationResult result = service.send(request());

        assertEquals(ManualNotificationFailure.SENDER_NOT_RESOLVED, result.failure());
        verify(audit).markFailed(eq(COMMUNICATION_ID), eq("SENDER_NOT_RESOLVED"), any());
        verify(delivery, times(1)).send(any());
    }

    private void arrangeReachableSingleOfficialGuardian(Guardian guardian) {
        arrangeStudentInTeacherScope();
        StudentGuardian relationship = officialRelationship(GUARDIAN_ID);
        when(relationships.findByStudentId(STUDENT_ID)).thenReturn(List.of(relationship));
        when(guardians.findByInstitutionIdAndIdIn(INSTITUTION_ID, java.util.Set.of(GUARDIAN_ID)))
                .thenReturn(List.of(guardian));
    }

    private void arrangeStudentInTeacherScope() {
        Student student = mock(Student.class);
        when(student.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(students.findStudentById(STUDENT_ID)).thenReturn(Optional.of(student));
        AcademicCourse course = mock(AcademicCourse.class);
        when(course.getId()).thenReturn(COURSE_ID);
        when(courses.findByInstitutionIdAndTeacherUserId(INSTITUTION_ID, TEACHER_ID))
                .thenReturn(List.of(course));
        CourseEnrollment enrollment = mock(CourseEnrollment.class);
        when(enrollment.getStudentId()).thenReturn(STUDENT_ID);
        when(enrollments.findEnrollmentsByCourseIdIn(java.util.Set.of(COURSE_ID)))
                .thenReturn(List.of(enrollment));
    }

    private static StudentGuardian officialRelationship(UUID guardianId) {
        StudentGuardian relationship = mock(StudentGuardian.class);
        when(relationship.getInstitutionId()).thenReturn(INSTITUTION_ID);
        when(relationship.getGuardianId()).thenReturn(guardianId);
        when(relationship.isOfficialLegalGuardian()).thenReturn(true);
        return relationship;
    }

    private static Guardian reachableGuardian(String externalUserId) {
        Guardian guardian = mock(Guardian.class);
        when(guardian.getId()).thenReturn(GUARDIAN_ID);
        when(guardian.getExternalUserId()).thenReturn(externalUserId);
        when(guardian.hasSystemAccess()).thenReturn(true);
        return guardian;
    }

    private static ManualNotificationRequest request() {
        return new ManualNotificationRequest(INSTITUTION_ID, TEACHER_ID, STUDENT_ID, "Subject", "<p>Content</p>");
    }
}
