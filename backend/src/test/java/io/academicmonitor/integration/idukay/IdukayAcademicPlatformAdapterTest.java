package io.academicmonitor.integration.idukay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.integration.idukay.activity.IdukayActivityDto;
import io.academicmonitor.integration.idukay.activity.IdukayCourseActivitiesClient;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.course.IdukayParentReferenceDto;
import io.academicmonitor.integration.idukay.course.IdukayStudentDto;
import io.academicmonitor.integration.idukay.course.IdukayStudentRelativeDto;
import io.academicmonitor.integration.idukay.course.IdukaySubjectDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.guardian.IdukayParentClient;
import io.academicmonitor.integration.idukay.guardian.IdukayParentDto;
import io.academicmonitor.integration.idukay.guardian.IdukayParentRelationalDataDto;
import io.academicmonitor.integration.idukay.guardian.IdukayParentUserDto;
import io.academicmonitor.integration.idukay.guardian.IdukayStudentDetailsClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import io.academicmonitor.integration.idukay.period.IdukayCustomYearDto;
import io.academicmonitor.integration.idukay.period.IdukayPartDto;
import io.academicmonitor.integration.idukay.period.IdukayTermDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
    private IdukayStudentDetailsClient studentDetailsClient;

    @Mock
    private IdukayParentClient parentClient;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private IdukayAuthenticatedSession session;

    private IdukayAcademicPlatformAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdukayAcademicPlatformAdapter(
                sessionProvider,
                coursesClient,
                activitiesClient,
                coursePeriodClient,
                studentDetailsClient,
                parentClient,
                guardianRepository,
                6,
                8);

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
        lenient().when(coursesClient.findTeacherCourses(session)).thenReturn(List.of(course));
        lenient().when(coursePeriodClient.findCustomYear(session, "course-001")).thenReturn(customYear);
        lenient().when(activitiesClient.findActivities(session, "course-001")).thenReturn(activities);
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

    @Test
    void fetchesSharedParentOnceAndMapsRelationshipsForSiblings() {
        IdukayStudentRelativeDto firstRelative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        IdukayStudentRelativeDto secondRelative = new IdukayStudentRelativeDto(
                "relation-beta",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(firstRelative)));
        when(studentDetailsClient.findStudent(session, "student-beta"))
                .thenReturn(new IdukayStudentDto("student-beta", null, List.of(secondRelative)));
        when(parentClient.findParent(session, "guardian-profile-alpha"))
                .thenReturn(new IdukayParentDto(
                        "guardian-profile-alpha",
                        new IdukayParentUserDto(
                                "guardian-user-alpha",
                                "Guardian",
                                null,
                                "One",
                                null,
                                "guardian.one@example.test",
                                true),
                        true,
                        new IdukayParentRelationalDataDto(null, null)));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha", "student-beta"));

        assertEquals(2, result.relationships().size());
        assertEquals(0, result.warnings());
        assertEquals(2, result.guardianStudentFetches());
        verify(parentClient, times(1)).findParent(session, "guardian-profile-alpha");
    }

    @Test
    void fetchesEachDistinctStudentDetailOnlyOnce() {
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of()));
        when(studentDetailsClient.findStudent(session, "student-beta"))
                .thenReturn(new IdukayStudentDto("student-beta", null, List.of()));

        var result = adapter.fetchGuardians(
                CONTEXT, List.of("student-alpha", " student-alpha ", "student-beta", "student-beta"));

        assertEquals(2, result.guardianStudentFetches());
        assertEquals(0, result.guardianStudentWarnings());
        verify(studentDetailsClient, times(1)).findStudent(session, "student-alpha");
        verify(studentDetailsClient, times(1)).findStudent(session, "student-beta");
    }

    @Test
    void doesNotRepeatStudentDetailAfterTheApiClientExhaustsItsRetries() {
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenThrow(new IdukayApiException("service unavailable", 503, null))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of()));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha"));

        assertEquals(1, result.guardianStudentFetches());
        assertEquals(1, result.guardianStudentWarnings());
        verify(studentDetailsClient, times(1)).findStudent(session, "student-alpha");
    }

    @Test
    void warnsAndContinuesWhenOneStudentDetailKeepsReturning503() {
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenThrow(new IdukayApiException("service unavailable", 503, null));
        when(studentDetailsClient.findStudent(session, "student-beta"))
                .thenReturn(new IdukayStudentDto("student-beta", null, List.of()));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha", "student-beta"));

        assertEquals(2, result.guardianStudentFetches());
        assertEquals(1, result.guardianStudentWarnings());
        assertEquals(1, result.warnings());
        verify(studentDetailsClient, times(1)).findStudent(session, "student-alpha");
        verify(studentDetailsClient).findStudent(session, "student-beta");
    }

    @Test
    void doesNotRetryA404StudentDetail() {
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenThrow(new IdukayApiException("not found", 404, null));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha"));

        assertEquals(1, result.guardianStudentWarnings());
        verify(studentDetailsClient, times(1)).findStudent(session, "student-alpha");
    }

    @Test
    void reusesCompleteGuardiansAndStillMapsCurrentRelationshipFlags() {
        IdukayStudentRelativeDto relative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                false,
                true,
                true,
                false,
                true);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(relative)));
        when(guardianRepository.findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                        CONTEXT.institutionId(), "IDUKAY", Set.of("guardian-profile-alpha")))
                .thenReturn(List.of(new Guardian(
                        CONTEXT.institutionId(),
                        "IDUKAY",
                        "guardian-profile-alpha",
                        "guardian-user-alpha",
                        "Guardian One",
                        "guardian.one@example.test",
                        true)));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha"));

        assertEquals(1, result.guardianCacheHits());
        assertEquals(0, result.guardianFetches());
        assertTrue(result.relationships().getFirst().legalGuardian());
        assertTrue(result.relationships().getFirst().economicRepresentative());
        verify(parentClient, never()).findParent(session, "guardian-profile-alpha");
    }

    @Test
    void refetchesAnIncompleteCachedGuardian() {
        IdukayStudentRelativeDto relative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        Guardian incomplete = org.mockito.Mockito.mock(Guardian.class);
        when(incomplete.getExternalId()).thenReturn("guardian-profile-alpha");
        when(incomplete.getExternalUserId()).thenReturn(null);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(relative)));
        when(guardianRepository.findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                        CONTEXT.institutionId(), "IDUKAY", Set.of("guardian-profile-alpha")))
                .thenReturn(List.of(incomplete));
        when(parentClient.findParent(session, "guardian-profile-alpha")).thenReturn(parent("guardian-profile-alpha"));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha"));

        assertEquals(0, result.guardianCacheHits());
        assertEquals(1, result.guardianFetches());
        verify(parentClient).findParent(session, "guardian-profile-alpha");
    }

    @Test
    void fetchesOnlyTheDistinctMissingParentsForOneHundredRelationships() {
        for (int index = 0; index < 100; index++) {
            String studentId = "student-" + index;
            String parentId = "guardian-profile-" + (index % 80);
            when(studentDetailsClient.findStudent(session, studentId))
                    .thenReturn(new IdukayStudentDto(
                            studentId,
                            null,
                            List.of(new IdukayStudentRelativeDto(
                                    "relationship-" + index,
                                    new IdukayParentReferenceDto(parentId),
                                    "Parent",
                                    true,
                                    true,
                                    false,
                                    true,
                                    true))));
        }
        when(parentClient.findParent(org.mockito.ArgumentMatchers.eq(session), anyString()))
                .thenAnswer(invocation -> parent(invocation.getArgument(1)));

        var result = adapter.fetchGuardians(
                CONTEXT,
                java.util.stream.IntStream.range(0, 100)
                        .mapToObj(index -> "student-" + index)
                        .toList());

        assertEquals(100, result.relationships().size());
        assertEquals(80, result.uniqueParentIds());
        assertEquals(80, result.guardianFetches());
        verify(parentClient, times(80)).findParent(org.mockito.ArgumentMatchers.eq(session), anyString());
    }

    @Test
    void doesNotRepeatParentDetailAfterTheApiClientExhaustsItsRetries() {
        IdukayStudentRelativeDto relative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(relative)));
        when(parentClient.findParent(session, "guardian-profile-alpha"))
                .thenThrow(new IdukayApiException("service unavailable", 503, null))
                .thenReturn(parent("guardian-profile-alpha"));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha"));

        assertEquals(0, result.relationships().size());
        assertEquals(1, result.warnings());
        verify(parentClient, times(1)).findParent(session, "guardian-profile-alpha");
    }

    @Test
    void warnsAndContinuesAfterPersistent503() {
        IdukayStudentRelativeDto failingRelative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        IdukayStudentRelativeDto successfulRelative = new IdukayStudentRelativeDto(
                "relation-beta",
                new IdukayParentReferenceDto("guardian-profile-beta"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(failingRelative)));
        when(studentDetailsClient.findStudent(session, "student-beta"))
                .thenReturn(new IdukayStudentDto("student-beta", null, List.of(successfulRelative)));
        when(parentClient.findParent(session, "guardian-profile-alpha"))
                .thenThrow(new IdukayApiException("service unavailable", 503, null));
        when(parentClient.findParent(session, "guardian-profile-beta")).thenReturn(parent("guardian-profile-beta"));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha", "student-beta"));

        assertEquals(1, result.relationships().size());
        assertEquals(1, result.warnings());
        verify(parentClient, times(1)).findParent(session, "guardian-profile-alpha");
    }

    @Test
    void doesNotRetryA404() {
        IdukayStudentRelativeDto relative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(relative)));
        when(parentClient.findParent(session, "guardian-profile-alpha"))
                .thenThrow(new IdukayApiException("not found", 404, null));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha"));

        assertEquals(0, result.relationships().size());
        assertEquals(1, result.warnings());
        verify(parentClient, times(1)).findParent(session, "guardian-profile-alpha");
    }

    @Test
    void boundsConcurrentParentRequests() {
        IdukayAcademicPlatformAdapter limitedAdapter = new IdukayAcademicPlatformAdapter(
                sessionProvider,
                coursesClient,
                activitiesClient,
                coursePeriodClient,
                studentDetailsClient,
                parentClient,
                guardianRepository,
                2,
                2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        for (int index = 0; index < 6; index++) {
            String studentId = "student-" + index;
            String parentId = "guardian-profile-" + index;
            when(studentDetailsClient.findStudent(session, studentId))
                    .thenReturn(new IdukayStudentDto(
                            studentId,
                            null,
                            List.of(new IdukayStudentRelativeDto(
                                    "relation-" + index,
                                    new IdukayParentReferenceDto(parentId),
                                    "Parent",
                                    true,
                                    true,
                                    false,
                                    true,
                                    true))));
        }
        when(parentClient.findParent(org.mockito.ArgumentMatchers.eq(session), anyString()))
                .thenAnswer(invocation -> {
                    int current = active.incrementAndGet();
                    maximumActive.accumulateAndGet(current, Math::max);
                    try {
                        Thread.sleep(40);
                    } finally {
                        active.decrementAndGet();
                    }
                    return parent(invocation.getArgument(1));
                });

        limitedAdapter.fetchGuardians(
                CONTEXT,
                java.util.stream.IntStream.range(0, 6)
                        .mapToObj(index -> "student-" + index)
                        .toList());

        assertTrue(maximumActive.get() <= 2);
    }

    @Test
    void boundsConcurrentStudentDetailRequests() {
        IdukayAcademicPlatformAdapter limitedAdapter = new IdukayAcademicPlatformAdapter(
                sessionProvider,
                coursesClient,
                activitiesClient,
                coursePeriodClient,
                studentDetailsClient,
                parentClient,
                guardianRepository,
                6,
                2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        for (int index = 0; index < 6; index++) {
            String studentId = "student-" + index;
            when(studentDetailsClient.findStudent(session, studentId)).thenAnswer(invocation -> {
                int current = active.incrementAndGet();
                maximumActive.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(40);
                } finally {
                    active.decrementAndGet();
                }
                return new IdukayStudentDto(studentId, null, List.of());
            });
        }

        limitedAdapter.fetchGuardians(
                CONTEXT,
                java.util.stream.IntStream.range(0, 6)
                        .mapToObj(index -> "student-" + index)
                        .toList());

        assertTrue(maximumActive.get() <= 2);
    }

    @Test
    void continuesGuardianSyncWhenAnIndividualParentRequestFails() {
        IdukayStudentRelativeDto firstRelative = new IdukayStudentRelativeDto(
                "relation-alpha",
                new IdukayParentReferenceDto("guardian-profile-alpha"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        IdukayStudentRelativeDto secondRelative = new IdukayStudentRelativeDto(
                "relation-beta",
                new IdukayParentReferenceDto("guardian-profile-beta"),
                "Parent",
                true,
                true,
                false,
                true,
                true);
        when(studentDetailsClient.findStudent(session, "student-alpha"))
                .thenReturn(new IdukayStudentDto("student-alpha", null, List.of(firstRelative)));
        when(studentDetailsClient.findStudent(session, "student-beta"))
                .thenReturn(new IdukayStudentDto("student-beta", null, List.of(secondRelative)));
        when(parentClient.findParent(session, "guardian-profile-alpha"))
                .thenReturn(new IdukayParentDto(
                        "guardian-profile-alpha",
                        new IdukayParentUserDto(
                                "guardian-user-alpha",
                                "Guardian",
                                null,
                                "One",
                                null,
                                "guardian.one@example.test",
                                true),
                        true,
                        new IdukayParentRelationalDataDto(null, null)));
        when(parentClient.findParent(session, "guardian-profile-beta"))
                .thenThrow(new IdukayApiException("Idukay API request failed with HTTP 503"));

        var result = adapter.fetchGuardians(CONTEXT, List.of("student-alpha", "student-beta"));

        assertEquals(1, result.relationships().size());
        assertEquals("guardian-profile-alpha", result.relationships().getFirst().guardianExternalId());
        assertEquals(1, result.warnings());
        verify(parentClient).findParent(session, "guardian-profile-alpha");
        verify(parentClient).findParent(session, "guardian-profile-beta");
    }

    private static IdukayParentDto parent(String parentId) {
        return new IdukayParentDto(
                parentId,
                new IdukayParentUserDto(
                        parentId + "-user", "Guardian", null, "One", null, "guardian@example.test", true),
                true,
                new IdukayParentRelationalDataDto(null, null));
    }
}
