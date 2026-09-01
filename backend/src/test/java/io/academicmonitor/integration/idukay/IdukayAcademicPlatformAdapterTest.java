package io.academicmonitor.integration.idukay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.integration.idukay.activity.IdukayActivityDto;
import io.academicmonitor.integration.idukay.activity.IdukayCourseActivitiesClient;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.course.IdukaySubjectDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import io.academicmonitor.integration.idukay.period.IdukayCustomYearDto;
import io.academicmonitor.integration.idukay.period.IdukayPartDto;
import io.academicmonitor.integration.idukay.period.IdukayTermDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdukayAcademicPlatformAdapterTest {

    private static final AcademicPlatformContext CONTEXT = new AcademicPlatformContext(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Mock
    private IdukaySessionProvider sessionProvider;

    @Mock
    private IdukayTeacherCoursesClient coursesClient;

    @Mock
    private IdukayCourseActivitiesClient activitiesClient;

    @Mock
    private IdukayCoursePeriodClient coursePeriodClient;

    @Mock
    private IdukayAuthenticatedSession session;

    private IdukayAcademicPlatformAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter =
                new IdukayAcademicPlatformAdapter(sessionProvider, coursesClient, activitiesClient, coursePeriodClient);

        IdukayTeacherCourseDto course = new IdukayTeacherCourseDto(
                "course-001",
                "Course",
                null,
                null,
                new IdukaySubjectDto("subject-001", "Physics"),
                "year-001",
                List.of());

        IdukayTermDto firstTerm = new IdukayTermDto(
                "term-first", "First term", "T1", List.of(new IdukayPartDto("part-first", "Part 1", "P1")));
        IdukayTermDto secondTerm = new IdukayTermDto(
                "term-second", "Second term", "T2", List.of(new IdukayPartDto("part-second", "Part 2", "P2")));
        IdukayCustomYearDto customYear = new IdukayCustomYearDto(
                "year-001",
                "Academic year 2026-2027",
                "2026-2027",
                new BigDecimal("10.00"),
                List.of(firstTerm, secondTerm));

        List<IdukayActivityDto> activities = List.of(
                new IdukayActivityDto("activity-first", "First activity", null, "part-first", List.of()),
                new IdukayActivityDto("activity-second", "Second activity", null, "part-second", List.of()),
                new IdukayActivityDto("activity-unresolved", "Unresolved activity", null, "unknown-part", List.of()));

        when(sessionProvider.getSession(CONTEXT)).thenReturn(session);
        when(coursesClient.findTeacherCourses(session)).thenReturn(List.of(course));
        when(coursePeriodClient.findCustomYear(session, "course-001")).thenReturn(customYear);
        when(activitiesClient.findActivities(session, "course-001")).thenReturn(activities);
    }

    @Test
    void filtersActivitiesByResolvedTermAndMapsAcademicCalendarInSourceOrder() {
        AcademicPlatformSnapshot snapshot = adapter.fetchSnapshot(CONTEXT, new AcademicPlatformFilter("term-first"));

        PlatformCourseSnapshot course = snapshot.courses().getFirst();
        assertEquals("year-001", course.academicYear().externalId());
        assertEquals("2026-2027", course.academicYear().year());
        assertEquals(new BigDecimal("10.00"), course.academicYear().baseScore());
        assertEquals(2, course.academicYear().periods().size());

        PlatformAcademicPeriodSnapshot firstPeriod =
                course.academicYear().periods().getFirst();
        PlatformAcademicPeriodSnapshot secondPeriod =
                course.academicYear().periods().get(1);
        assertEquals("term-first", firstPeriod.externalId());
        assertEquals(1, firstPeriod.order());
        assertEquals("term-second", secondPeriod.externalId());
        assertEquals(2, secondPeriod.order());

        assertEquals(1, course.activities().size());
        assertEquals("activity-first", course.activities().getFirst().externalId());
        assertEquals("term-first", course.activities().getFirst().periodExternalId());

        verify(activitiesClient).findActivities(session, "course-001");
    }

    @Test
    void keepsUnresolvedActivityUnassignedWhenSynchronizationIsNotFiltered() {
        AcademicPlatformSnapshot snapshot = adapter.fetchSnapshot(CONTEXT);

        PlatformCourseSnapshot course = snapshot.courses().getFirst();
        assertEquals(3, course.activities().size());
        assertEquals("activity-unresolved", course.activities().get(2).externalId());
        assertNull(course.activities().get(2).periodExternalId());
    }
}
