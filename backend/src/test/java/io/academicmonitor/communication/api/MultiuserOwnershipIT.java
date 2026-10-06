package io.academicmonitor.communication.api;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.academicmonitor.academic.domain.*;
import io.academicmonitor.communication.application.AlertCommunicationService;
import io.academicmonitor.communication.domain.Communication;
import io.academicmonitor.dashboard.api.AcademicDashboardController;
import io.academicmonitor.dashboard.application.AcademicDashboardQueryService;
import io.academicmonitor.identity.application.AcademicMonitorPrincipal;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.institution.domain.*;
import io.academicmonitor.monitoring.api.AlertInboxController;
import io.academicmonitor.monitoring.application.AlertInboxQueryService;
import io.academicmonitor.monitoring.application.AlertTriageService;
import io.academicmonitor.monitoring.domain.Alert;
import io.academicmonitor.monitoring.domain.AlertSeverity;
import io.academicmonitor.notification.api.AppNotificationController;
import io.academicmonitor.notification.application.AppNotificationService;
import io.academicmonitor.notification.domain.*;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Real PostgreSQL repositories and application services exercise ownership behind the HTTP boundary. */
class MultiuserOwnershipIT extends PostgresIntegrationTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Autowired
    private AcademicDashboardQueryService dashboard;

    @Autowired
    private AlertInboxQueryService inbox;

    @Autowired
    private AlertTriageService triage;

    @Autowired
    private AlertCommunicationService communications;

    @Autowired
    private AppNotificationService notifications;

    private MockMvc mvc;
    private Fixture teacherA;
    private Fixture teacherB;
    private Fixture teacherC;

    @BeforeEach
    void setUp() {
        Institution institutionA = persist(new Institution("Institution A", "America/Guayaquil"));
        Institution institutionB = persist(new Institution("Institution B", "America/Guayaquil"));
        teacherA = fixture(institutionA, "A");
        teacherB = fixture(institutionA, "B");
        teacherC = fixture(institutionB, "C");
        var context = new AuthenticatedAcademicContext();
        mvc = MockMvcBuilders.standaloneSetup(
                        new AcademicDashboardController(dashboard, context),
                        new AlertInboxController(inbox, triage, communications, context),
                        new CommunicationController(communications, context),
                        new AppNotificationController(notifications, context))
                .setControllerAdvice(
                        new CommunicationExceptionHandler(),
                        applicationContext.getBean("appNotificationExceptionHandler"))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherAReceivesOnlyOwnCoursesAlertsCommunicationsAndNotificationsDespiteSpoofedIds() throws Exception {
        authenticate(teacherA);
        for (Fixture foreign : List.of(teacherB, teacherC)) {
            mvc.perform(spoof(get("/api/v1/dashboard"), foreign))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.courses.length()").value(1))
                    .andExpect(jsonPath("$.courses[0].id")
                            .value(teacherA.course().getId().toString()));
            mvc.perform(spoof(get("/api/v1/alerts"), foreign))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts.length()").value(1))
                    .andExpect(jsonPath("$.alerts[0].id")
                            .value(teacherA.alert().getId().toString()));
            mvc.perform(spoof(get("/api/v1/communications"), foreign))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id")
                            .value(teacherA.communication().getId().toString()));
            mvc.perform(spoof(get("/api/v1/notifications"), foreign))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].id")
                            .value(teacherA.notification().getId().toString()));
        }
    }

    @Test
    void foreignResourceIdsRemainOutsideScopeForBothAnotherTeacherAndAnotherInstitution() throws Exception {
        authenticate(teacherA);
        for (Fixture foreign : List.of(teacherB, teacherC)) {
            mvc.perform(spoof(
                            get(
                                    "/api/v1/communications/{id}",
                                    foreign.communication().getId()),
                            foreign))
                    .andExpect(status().isNotFound());
            mvc.perform(spoof(
                                    patch(
                                            "/api/v1/communications/{id}",
                                            foreign.communication().getId()),
                                    foreign)
                            .contentType("application/json")
                            .content("{\"subject\":\"Changed\",\"content\":\"Changed\"}"))
                    .andExpect(status().isNotFound());
            mvc.perform(spoof(
                            delete(
                                    "/api/v1/communications/{id}",
                                    foreign.communication().getId()),
                            foreign))
                    .andExpect(status().isNotFound());
            mvc.perform(spoof(
                            post(
                                    "/api/v1/notifications/{id}/read",
                                    foreign.notification().getId()),
                            foreign))
                    .andExpect(status().isNotFound());
            mvc.perform(spoof(
                            post(
                                    "/api/v1/alerts/{id}/acknowledge",
                                    foreign.alert().getId()),
                            foreign))
                    .andExpect(status().isNotFound());
            mvc.perform(spoof(get("/api/v1/alerts"), foreign)
                            .queryParam("courseId", foreign.course().getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts.length()").value(0));
            mvc.perform(spoof(get("/api/v1/dashboard"), foreign)
                            .queryParam(
                                    "academicPeriodId", foreign.period().getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.courses.length()").value(0));
            assertNull(foreign.notification().getReadAt());
            assertNull(foreign.alert().getAcknowledgedAt());
        }
    }

    @Test
    void eachTeacherRestoresOwnScopeWhenThePrincipalChanges() throws Exception {
        for (Fixture teacher : List.of(teacherA, teacherB, teacherC)) {
            authenticate(teacher);
            mvc.perform(get("/api/v1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.courses.length()").value(1))
                    .andExpect(jsonPath("$.courses[0].id")
                            .value(teacher.course().getId().toString()));
            mvc.perform(get("/api/v1/alerts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts.length()").value(1))
                    .andExpect(jsonPath("$.alerts[0].id")
                            .value(teacher.alert().getId().toString()));
            mvc.perform(get("/api/v1/communications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id")
                            .value(teacher.communication().getId().toString()));
            mvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].id")
                            .value(teacher.notification().getId().toString()));
        }
    }

    private void authenticate(Fixture fixture) {
        var principal = new AcademicMonitorPrincipal(
                fixture.user().getId(), fixture.institution().getId(), SystemRole.USER, InstitutionRole.TEACHER);
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    private MockHttpServletRequestBuilder spoof(MockHttpServletRequestBuilder request, Fixture foreign) {
        return request.queryParam("institutionId", foreign.institution().getId().toString())
                .queryParam("teacherUserId", foreign.user().getId().toString());
    }

    private Fixture fixture(Institution institution, String suffix) {
        UUID institutionId = institution.getId();
        User user = persist(new User("teacher-" + suffix + "-" + UUID.randomUUID() + "@example.test"));
        persist(new InstitutionMembership(user.getId(), institutionId, InstitutionRole.TEACHER));
        AcademicYear year =
                persist(new AcademicYear(institutionId, "TEST", suffix, "Year", "2026-2027", BigDecimal.TEN));
        AcademicPeriod period = persist(new AcademicPeriod(year.getId(), suffix, "Period", "P1", 1));
        AcademicCourse course = persist(new AcademicCourse(
                institutionId, user.getId(), year.getId(), "TEST", suffix, "Course " + suffix, "Physics"));
        Student student = persist(new Student(institutionId, "TEST", suffix, "Student", suffix));
        Guardian guardian =
                persist(new Guardian(institutionId, "TEST", suffix, "recipient-" + suffix, "Guardian", null, true));
        Activity activity = persist(new Activity(
                course.getId(), period.getId(), "TEST", suffix, "Activity", BigDecimal.TEN, LocalDate.of(2026, 10, 1)));
        Alert alert = persist(new Alert(
                institutionId,
                course.getId(),
                activity.getId(),
                student.getId(),
                "LOW_GRADE",
                AlertSeverity.CRITICAL,
                new BigDecimal("4.00")));
        Communication communication = persist(Communication.draft(
                institutionId,
                user.getId(),
                student.getId(),
                guardian.getId(),
                alert.getId(),
                "IDUKAY",
                "Subject " + suffix,
                "Content " + suffix));
        AppNotification notification = persist(new AppNotification(
                institutionId,
                user.getId(),
                AppNotificationType.SYNC_COMPLETED,
                "Notification " + suffix,
                "Sync complete",
                AppNotificationReferenceType.SYNC,
                null,
                period.getId(),
                null));
        return new Fixture(institution, user, course, period, alert, communication, notification);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    private record Fixture(
            Institution institution,
            User user,
            AcademicCourse course,
            AcademicPeriod period,
            Alert alert,
            Communication communication,
            AppNotification notification) {}
}
