package io.academicmonitor.integration.idukay.test;

import io.academicmonitor.academic.application.AcademicBatchSyncResult;
import io.academicmonitor.academic.application.AcademicSyncService;
import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.integration.idukay.IdukayAcademicPlatformAdapter;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import io.academicmonitor.integration.idukay.period.IdukayCustomYearDto;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/idukay")
@ConditionalOnProperty(prefix = "app.idukay", name = "test-login-enabled", havingValue = "true")
public class IdukayTestSnapshotController {

    private static final String PLATFORM_CODE = "IDUKAY";

    private final IdukayAcademicPlatformAdapter adapter;
    private final AcademicSyncService syncService;
    private final IdukaySessionProvider sessionProvider;
    private final IdukayTeacherCoursesClient teacherCoursesClient;
    private final IdukayCoursePeriodClient coursePeriodClient;

    public IdukayTestSnapshotController(
            IdukayAcademicPlatformAdapter adapter,
            AcademicSyncService syncService,
            IdukaySessionProvider sessionProvider,
            IdukayTeacherCoursesClient teacherCoursesClient,
            IdukayCoursePeriodClient coursePeriodClient) {

        this.adapter = adapter;
        this.syncService = syncService;
        this.sessionProvider = sessionProvider;
        this.teacherCoursesClient = teacherCoursesClient;
        this.coursePeriodClient = coursePeriodClient;
    }

    @GetMapping("/test-periods")
    public TestPeriodsResponse testPeriods(@RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {

        AcademicPlatformContext context = new AcademicPlatformContext(institutionId, teacherUserId);

        IdukayAuthenticatedSession session = sessionProvider.getSession(context);

        List<IdukayTeacherCourseDto> courses = teacherCoursesClient.findTeacherCourses(session);

        if (courses.isEmpty()) {
            throw new IllegalStateException("No Idukay teacher courses are available");
        }

        IdukayCustomYearDto customYear =
                coursePeriodClient.findCustomYear(session, courses.get(0).id());

        List<TestPeriodResponse> periods = customYear.terms().stream()
                .map(term -> new TestPeriodResponse(term.id(), term.name(), term.abbreviation()))
                .toList();

        return new TestPeriodsResponse(customYear.id(), customYear.name(), customYear.baseScore(), periods);
    }

    @GetMapping("/test-snapshot")
    public TestSnapshotResponse testSnapshot(@RequestParam UUID institutionId, @RequestParam UUID teacherUserId) {

        AcademicPlatformContext context = new AcademicPlatformContext(institutionId, teacherUserId);

        AcademicPlatformSnapshot snapshot = adapter.fetchSnapshot(context);

        int courses = snapshot.courses().size();

        int students = snapshot.courses().stream()
                .mapToInt(course -> course.students().size())
                .sum();

        int activities = snapshot.courses().stream()
                .mapToInt(course -> course.activities().size())
                .sum();

        int grades = snapshot.courses().stream()
                .flatMap(course -> course.activities().stream())
                .mapToInt(activity -> activity.grades().size())
                .sum();

        return new TestSnapshotResponse("OK", courses, students, activities, grades);
    }

    @GetMapping("/test-filtered-snapshot")
    public TestFilteredSnapshotResponse testFilteredSnapshot(
            @RequestParam UUID institutionId, @RequestParam UUID teacherUserId, @RequestParam String periodExternalId) {

        AcademicPlatformContext context = new AcademicPlatformContext(institutionId, teacherUserId);

        AcademicPlatformFilter filter = new AcademicPlatformFilter(periodExternalId);

        AcademicPlatformSnapshot snapshot = adapter.fetchSnapshot(context, filter);

        int activities = snapshot.courses().stream()
                .mapToInt(course -> course.activities().size())
                .sum();

        int grades = snapshot.courses().stream()
                .flatMap(course -> course.activities().stream())
                .mapToInt(activity -> activity.grades().size())
                .sum();

        return new TestFilteredSnapshotResponse(
                periodExternalId, snapshot.courses().size(), activities, grades);
    }

    @PostMapping("/test-sync")
    public TestBatchSyncResponse testSync(
            @RequestParam UUID institutionId, @RequestParam UUID teacherUserId, @RequestParam String periodExternalId) {

        AcademicPlatformFilter filter = new AcademicPlatformFilter(periodExternalId);

        AcademicBatchSyncResult result =
                syncService.synchronizeAll(institutionId, teacherUserId, PLATFORM_CODE, adapter, filter);

        return new TestBatchSyncResponse(
                result.academicPeriodId(),
                result.coursesProcessed(),
                result.gradesProcessed(),
                result.openAlerts(),
                result.warnings(),
                result.critical());
    }

    public record TestPeriodsResponse(
            String academicYearId,
            String academicYear,
            java.math.BigDecimal baseScore,
            List<TestPeriodResponse> periods) {}

    public record TestPeriodResponse(String id, String name, String abbreviation) {}

    public record TestSnapshotResponse(String status, int courses, int students, int activities, int grades) {}

    public record TestFilteredSnapshotResponse(String periodExternalId, int courses, int activities, int grades) {}

    public record TestBatchSyncResponse(
            UUID academicPeriodId,
            int coursesProcessed,
            int gradesProcessed,
            int openAlerts,
            long warnings,
            long critical) {}
}
