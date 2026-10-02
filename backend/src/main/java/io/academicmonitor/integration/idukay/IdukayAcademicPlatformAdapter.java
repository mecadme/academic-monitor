package io.academicmonitor.integration.idukay;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicYearSnapshot;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformGuardianSyncSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentGuardianSnapshot;
import io.academicmonitor.academic.domain.Guardian;
import io.academicmonitor.academic.domain.GuardianRepository;
import io.academicmonitor.integration.idukay.activity.IdukayActivityDto;
import io.academicmonitor.integration.idukay.activity.IdukayActivityMapper;
import io.academicmonitor.integration.idukay.activity.IdukayCourseActivitiesClient;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.course.IdukayCourseMapper;
import io.academicmonitor.integration.idukay.course.IdukayStudentDto;
import io.academicmonitor.integration.idukay.course.IdukayStudentRelativeDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.guardian.IdukayGuardianMapper;
import io.academicmonitor.integration.idukay.guardian.IdukayParentClient;
import io.academicmonitor.integration.idukay.guardian.IdukayParentDto;
import io.academicmonitor.integration.idukay.guardian.IdukayStudentDetailsClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import io.academicmonitor.integration.idukay.period.IdukayCustomYearDto;
import io.academicmonitor.integration.idukay.period.IdukayPeriodResolver;
import io.academicmonitor.integration.idukay.period.IdukayTermDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IdukayAcademicPlatformAdapter implements AcademicPlatformPort {

    private static final String PLATFORM_CODE = "IDUKAY";
    private static final Logger LOGGER = LoggerFactory.getLogger(IdukayAcademicPlatformAdapter.class);

    private final IdukaySessionProvider sessionProvider;
    private final IdukayTeacherCoursesClient coursesClient;
    private final IdukayCourseActivitiesClient activitiesClient;
    private final IdukayCoursePeriodClient coursePeriodClient;
    private final IdukayStudentDetailsClient studentDetailsClient;
    private final IdukayParentClient parentClient;
    private final GuardianRepository guardianRepository;
    private final int guardianFetchConcurrency;
    private final int guardianStudentFetchConcurrency;

    public IdukayAcademicPlatformAdapter(
            IdukaySessionProvider sessionProvider,
            IdukayTeacherCoursesClient coursesClient,
            IdukayCourseActivitiesClient activitiesClient,
            IdukayCoursePeriodClient coursePeriodClient,
            IdukayStudentDetailsClient studentDetailsClient,
            IdukayParentClient parentClient,
            GuardianRepository guardianRepository,
            @Value("${app.idukay.guardian-fetch-concurrency:6}") int guardianFetchConcurrency,
            @Value("${app.idukay.guardian-student-fetch-concurrency:8}") int guardianStudentFetchConcurrency) {

        this.sessionProvider = sessionProvider;
        this.coursesClient = coursesClient;
        this.activitiesClient = activitiesClient;
        this.coursePeriodClient = coursePeriodClient;
        this.studentDetailsClient = studentDetailsClient;
        this.parentClient = parentClient;
        this.guardianRepository = guardianRepository;
        if (guardianFetchConcurrency < 1) {
            throw new IllegalArgumentException("guardianFetchConcurrency must be positive");
        }
        if (guardianStudentFetchConcurrency < 1) {
            throw new IllegalArgumentException("guardianStudentFetchConcurrency must be positive");
        }
        this.guardianFetchConcurrency = guardianFetchConcurrency;
        this.guardianStudentFetchConcurrency = guardianStudentFetchConcurrency;
    }

    @Override
    public PlatformGuardianSyncSnapshot fetchGuardians(
            AcademicPlatformContext context, Collection<String> studentExternalIds) {
        if (studentExternalIds == null || studentExternalIds.isEmpty()) return PlatformGuardianSyncSnapshot.empty();
        long startedAt = System.nanoTime();
        IdukayAuthenticatedSession session = sessionProvider.getSession(context);
        long studentFetchStartedAt = System.nanoTime();
        StudentFetchResult studentFetchResult = fetchStudents(session, studentExternalIds);
        long studentFetchDurationMs =
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - studentFetchStartedAt);
        Map<String, List<IdukayStudentRelativeDto>> relativesByStudent = new LinkedHashMap<>();
        Set<String> parentIds = new java.util.LinkedHashSet<>();
        int warnings = studentFetchResult.warnings();
        for (Map.Entry<String, IdukayStudentDto> entry :
                studentFetchResult.studentsById().entrySet()) {
            String studentId = entry.getKey();
            IdukayStudentDto student = entry.getValue();
            if (student == null) {
                continue;
            }
            List<IdukayStudentRelativeDto> relatives = student.relatives();
            relativesByStudent.put(studentId, relatives);
            for (IdukayStudentRelativeDto relative : relatives) {
                if (relative == null
                        || relative.parent() == null
                        || relative.parent().id() == null
                        || relative.parent().id().isBlank()) {
                    warnings++;
                    continue;
                }
                parentIds.add(relative.parent().id().trim());
            }
        }
        List<Guardian> cachedGuardians = parentIds.isEmpty()
                ? List.of()
                : guardianRepository.findByInstitutionIdAndPlatformCodeAndExternalIdIn(
                        context.institutionId(), PLATFORM_CODE, parentIds);
        Map<String, Guardian> cachedGuardiansById = (cachedGuardians == null ? List.<Guardian>of() : cachedGuardians)
                .stream()
                        .filter(IdukayAcademicPlatformAdapter::isCompleteGuardian)
                        .collect(java.util.stream.Collectors.toMap(
                                Guardian::getExternalId,
                                guardian -> guardian,
                                (first, ignored) -> first,
                                LinkedHashMap::new));
        Map<String, IdukayParentDto> parentsById = new LinkedHashMap<>();
        Set<String> parentIdsToFetch = parentIds.stream()
                .filter(parentId -> !cachedGuardiansById.containsKey(parentId))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        ParentFetchResult parentFetchResult = fetchParents(session, parentIdsToFetch);
        parentsById.putAll(parentFetchResult.parentsById());
        warnings += parentFetchResult.warnings();
        int guardianCacheHits = cachedGuardiansById.size();
        int guardianFetches = parentIdsToFetch.size();
        List<PlatformStudentGuardianSnapshot> relationships = new ArrayList<>();
        for (Map.Entry<String, List<IdukayStudentRelativeDto>> entry : relativesByStudent.entrySet()) {
            for (IdukayStudentRelativeDto relative : entry.getValue()) {
                if (relative == null
                        || relative.parent() == null
                        || relative.parent().id() == null
                        || relative.parent().id().isBlank()) continue;
                String parentId = relative.parent().id().trim();
                if (!cachedGuardiansById.containsKey(parentId) && !parentsById.containsKey(parentId)) continue;
                PlatformStudentGuardianSnapshot mapped = mapGuardianRelationship(
                        entry.getKey(), relative, cachedGuardiansById.get(parentId), parentsById.get(parentId));
                if (mapped == null) warnings++;
                else relationships.add(mapped);
            }
        }
        long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        return new PlatformGuardianSyncSnapshot(
                relationships,
                warnings,
                studentFetchResult.studentsInspected(),
                studentFetchResult.studentsInspected(),
                studentFetchResult.warnings(),
                studentFetchDurationMs,
                parentIds.size(),
                guardianCacheHits,
                guardianFetches,
                durationMs);
    }

    private StudentFetchResult fetchStudents(
            IdukayAuthenticatedSession session, Collection<String> studentExternalIds) {
        Set<String> uniqueStudentIds = new java.util.LinkedHashSet<>();
        int warnings = 0;
        for (String candidate : studentExternalIds) {
            if (candidate == null || candidate.isBlank()) {
                warnings++;
            } else {
                uniqueStudentIds.add(candidate.trim());
            }
        }
        if (uniqueStudentIds.isEmpty()) return new StudentFetchResult(Map.of(), warnings, 0);

        ExecutorService executor = Executors.newFixedThreadPool(guardianStudentFetchConcurrency);
        try {
            Map<String, Future<StudentFetch>> futuresById = new LinkedHashMap<>();
            for (String studentId : uniqueStudentIds) {
                futuresById.put(studentId, executor.submit(fetchStudent(session, studentId)));
            }
            Map<String, IdukayStudentDto> studentsById = new LinkedHashMap<>();
            for (Map.Entry<String, Future<StudentFetch>> entry : futuresById.entrySet()) {
                try {
                    StudentFetch result = entry.getValue().get();
                    if (result.student() == null) {
                        warnings++;
                    } else {
                        studentsById.put(entry.getKey(), result.student());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    warnings++;
                } catch (ExecutionException exception) {
                    LOGGER.warn("Idukay student-detail request could not be completed");
                    warnings++;
                }
            }
            return new StudentFetchResult(studentsById, warnings, uniqueStudentIds.size());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<StudentFetch> fetchStudent(IdukayAuthenticatedSession session, String studentId) {
        return () -> {
            try {
                return new StudentFetch(studentDetailsClient.findStudent(session, studentId));
            } catch (RuntimeException exception) {
                LOGGER.warn("Idukay student-detail request could not be completed");
                return new StudentFetch(null);
            }
        };
    }

    private ParentFetchResult fetchParents(IdukayAuthenticatedSession session, Set<String> parentIds) {
        if (parentIds.isEmpty()) return new ParentFetchResult(Map.of(), 0);
        ExecutorService executor = Executors.newFixedThreadPool(guardianFetchConcurrency);
        try {
            Map<String, Future<ParentFetch>> futuresById = new LinkedHashMap<>();
            for (String parentId : parentIds) {
                futuresById.put(parentId, executor.submit(fetchParent(session, parentId)));
            }
            Map<String, IdukayParentDto> parents = new LinkedHashMap<>();
            int warnings = 0;
            for (Map.Entry<String, Future<ParentFetch>> entry : futuresById.entrySet()) {
                try {
                    ParentFetch result = entry.getValue().get();
                    if (result.parent() == null) warnings++;
                    else parents.put(entry.getKey(), result.parent());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    warnings++;
                } catch (ExecutionException exception) {
                    warnings++;
                }
            }
            return new ParentFetchResult(parents, warnings);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<ParentFetch> fetchParent(IdukayAuthenticatedSession session, String parentId) {
        return () -> {
            try {
                return new ParentFetch(parentClient.findParent(session, parentId));
            } catch (IdukayApiException exception) {
                LOGGER.warn("Idukay parent-detail request could not be completed");
                return new ParentFetch(null);
            }
        };
    }

    private static boolean isCompleteGuardian(Guardian guardian) {
        return guardian != null
                && guardian.getExternalId() != null
                && !guardian.getExternalId().isBlank()
                && guardian.getExternalUserId() != null
                && !guardian.getExternalUserId().isBlank();
    }

    private static PlatformStudentGuardianSnapshot mapGuardianRelationship(
            String studentId, IdukayStudentRelativeDto relative, Guardian cachedGuardian, IdukayParentDto parent) {
        if (cachedGuardian != null) {
            return new PlatformStudentGuardianSnapshot(
                    studentId,
                    cachedGuardian.getExternalId(),
                    cachedGuardian.getExternalUserId(),
                    cachedGuardian.getDisplayName(),
                    cachedGuardian.getEmail(),
                    cachedGuardian.hasSystemAccess(),
                    relative.relationship(),
                    Boolean.TRUE.equals(relative.officialLegalGuardian()),
                    Boolean.TRUE.equals(relative.legalGuardian()),
                    Boolean.TRUE.equals(relative.economicRepresentative()),
                    Boolean.TRUE.equals(relative.canPickUp()),
                    Boolean.TRUE.equals(relative.livesWith()));
        }
        return parent == null ? null : IdukayGuardianMapper.toSnapshot(studentId, relative, parent);
    }

    private record StudentFetch(IdukayStudentDto student) {}

    private record StudentFetchResult(
            Map<String, IdukayStudentDto> studentsById, int warnings, int studentsInspected) {}

    private record ParentFetch(IdukayParentDto parent) {}

    private record ParentFetchResult(Map<String, IdukayParentDto> parentsById, int warnings) {}

    @Override
    public AcademicPlatformSnapshot fetchSnapshot(AcademicPlatformContext context) {

        return fetchSnapshot(context, AcademicPlatformFilter.all());
    }

    @Override
    public AcademicPlatformSnapshot fetchSnapshot(AcademicPlatformContext context, AcademicPlatformFilter filter) {

        IdukayAuthenticatedSession session = sessionProvider.getSession(context);

        AcademicPlatformFilter effectiveFilter = filter == null ? AcademicPlatformFilter.all() : filter;

        List<PlatformCourseSnapshot> courses = coursesClient.findTeacherCourses(session).stream()
                .map(course -> mapCourse(session, course, effectiveFilter))
                .toList();

        return new AcademicPlatformSnapshot(courses);
    }

    private PlatformCourseSnapshot mapCourse(
            IdukayAuthenticatedSession session, IdukayTeacherCourseDto course, AcademicPlatformFilter filter) {

        PlatformCourseSnapshot base = IdukayCourseMapper.toSnapshot(course);

        IdukayCustomYearDto customYear = coursePeriodClient.findCustomYear(session, course.id());

        PlatformAcademicYearSnapshot academicYear = toAcademicYearSnapshot(customYear);

        List<ResolvedActivity> resolvedActivities = activitiesClient.findActivities(session, course.id()).stream()
                .map(activity -> new ResolvedActivity(
                        activity,
                        IdukayPeriodResolver.findTermByPartId(customYear, activity.partId())
                                .orElse(null)))
                .toList();

        if (filter.hasPeriod()) {
            resolvedActivities = resolvedActivities.stream()
                    .filter(activity -> activity.term() != null
                            && filter.periodExternalId().equals(activity.term().id()))
                    .toList();
        }

        List<PlatformActivitySnapshot> activities = resolvedActivities.stream()
                .map(activity -> IdukayActivityMapper.toSnapshot(
                        activity.activity(),
                        customYear.baseScore(),
                        activity.term() == null ? null : activity.term().id()))
                .toList();

        return new PlatformCourseSnapshot(
                base.externalId(), base.name(), base.subject(), academicYear, activities, base.students());
    }

    private static PlatformAcademicYearSnapshot toAcademicYearSnapshot(IdukayCustomYearDto customYear) {
        if (customYear == null) {
            throw new IdukayApiException("Idukay did not return a custom year for the requested course");
        }

        String externalId = requireText(customYear.id(), "custom year._id");
        String name = requireText(customYear.name(), "custom year.name");
        BigDecimal baseScore = requirePositive(customYear.baseScore(), "custom year.base_score");

        List<PlatformAcademicPeriodSnapshot> periods = IntStream.range(
                        0, customYear.terms().size())
                .mapToObj(index -> toAcademicPeriodSnapshot(customYear.terms().get(index), index + 1))
                .toList();

        return new PlatformAcademicYearSnapshot(externalId, name, optionalText(customYear.year()), baseScore, periods);
    }

    private static PlatformAcademicPeriodSnapshot toAcademicPeriodSnapshot(IdukayTermDto term, int order) {
        if (term == null) {
            throw new IdukayApiException("Idukay custom year contained an empty term");
        }

        return new PlatformAcademicPeriodSnapshot(
                requireText(term.id(), "term._id"),
                requireText(term.name(), "term.name"),
                optionalText(term.abbreviation()),
                order);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IdukayApiException("Idukay response did not contain " + field);
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null) {
            throw new IdukayApiException("Idukay response did not contain " + field);
        }
        if (value.signum() <= 0) {
            throw new IdukayApiException("Idukay " + field + " must be greater than zero");
        }
        return value;
    }

    private record ResolvedActivity(IdukayActivityDto activity, IdukayTermDto term) {}
}
