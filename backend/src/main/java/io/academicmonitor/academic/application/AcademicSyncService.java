package io.academicmonitor.academic.application;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.domain.AcademicCourse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicSyncService {

    private final AcademicCalendarSynchronizer academicCalendarSynchronizer;
    private final CourseRosterSynchronizer courseRosterSynchronizer;
    private final ActivityGradeSynchronizer activityGradeSynchronizer;
    private final SyncAlertSummaryService alertSummaryService;

    public AcademicSyncService(
            AcademicCalendarSynchronizer academicCalendarSynchronizer,
            CourseRosterSynchronizer courseRosterSynchronizer,
            ActivityGradeSynchronizer activityGradeSynchronizer,
            SyncAlertSummaryService alertSummaryService) {
        this.academicCalendarSynchronizer = academicCalendarSynchronizer;
        this.courseRosterSynchronizer = courseRosterSynchronizer;
        this.activityGradeSynchronizer = activityGradeSynchronizer;
        this.alertSummaryService = alertSummaryService;
    }

    @Transactional
    public AcademicSyncResult synchronize(
            UUID institutionId, UUID teacherUserId, String platformCode, AcademicPlatformPort platform) {

        return synchronize(institutionId, teacherUserId, platformCode, platform, AcademicPlatformFilter.all());
    }

    @Transactional
    public AcademicSyncResult synchronize(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            AcademicPlatformPort platform,
            AcademicPlatformFilter filter) {

        AcademicPlatformSnapshot snapshot = fetchSnapshot(institutionId, teacherUserId, platformCode, platform, filter);

        PlatformCourseSnapshot platformCourse = resolveSingleCourse(snapshot);

        return synchronizePlatformCourse(institutionId, teacherUserId, platformCode, platformCourse);
    }

    @Transactional
    public AcademicBatchSyncResult synchronizeAll(
            UUID institutionId, UUID teacherUserId, String platformCode, AcademicPlatformPort platform) {

        return synchronizeAll(institutionId, teacherUserId, platformCode, platform, AcademicPlatformFilter.all());
    }

    @Transactional
    public AcademicBatchSyncResult synchronizeAll(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            AcademicPlatformPort platform,
            AcademicPlatformFilter filter) {

        AcademicPlatformSnapshot snapshot = fetchSnapshot(institutionId, teacherUserId, platformCode, platform, filter);

        validateSnapshot(snapshot);

        List<AcademicSyncResult> results = new ArrayList<>();

        for (PlatformCourseSnapshot platformCourse : snapshot.courses()) {

            AcademicSyncResult result =
                    synchronizePlatformCourse(institutionId, teacherUserId, platformCode, platformCourse);

            results.add(result);
        }

        return new AcademicBatchSyncResult(results);
    }

    private AcademicSyncResult synchronizePlatformCourse(
            UUID institutionId, UUID teacherUserId, String platformCode, PlatformCourseSnapshot platformCourse) {

        AcademicCalendarSynchronizer.Result calendarResult =
                academicCalendarSynchronizer.synchronize(institutionId, platformCode, platformCourse.academicYear());

        AcademicCourse course = courseRosterSynchronizer.synchronize(
                institutionId, teacherUserId, platformCode, platformCourse, calendarResult.academicYearId());

        ActivityGradeSynchronizer.Result processingResult = activityGradeSynchronizer.synchronize(
                institutionId, platformCode, course, platformCourse, calendarResult.periodIdsByExternalId());

        SyncAlertSummaryService.Summary alertSummary =
                alertSummaryService.summarize(course.getId(), processingResult.activityIds());

        return new AcademicSyncResult(
                course.getId(),
                course.getName(),
                platformCourse.students().size(),
                processingResult.gradesProcessed(),
                alertSummary.openAlerts(),
                alertSummary.warnings(),
                alertSummary.critical());
    }

    private AcademicPlatformSnapshot fetchSnapshot(
            UUID institutionId,
            UUID teacherUserId,
            String platformCode,
            AcademicPlatformPort platform,
            AcademicPlatformFilter filter) {

        requireId(institutionId, "institutionId");

        requireId(teacherUserId, "teacherUserId");

        requireText(platformCode, "platformCode");

        if (platform == null) {
            throw new IllegalArgumentException("platform is required");
        }

        AcademicPlatformFilter effectiveFilter = filter == null ? AcademicPlatformFilter.all() : filter;

        AcademicPlatformContext context = new AcademicPlatformContext(institutionId, teacherUserId);

        return platform.fetchSnapshot(context, effectiveFilter);
    }

    private PlatformCourseSnapshot resolveSingleCourse(AcademicPlatformSnapshot snapshot) {

        validateSnapshot(snapshot);

        return snapshot.courses().getFirst();
    }

    private static void validateSnapshot(AcademicPlatformSnapshot snapshot) {

        if (snapshot == null) {
            throw new IllegalStateException("Academic platform returned no snapshot");
        }

        if (snapshot.courses().isEmpty()) {
            throw new IllegalStateException("Academic platform returned no courses");
        }
    }

    private static UUID requireId(UUID value, String field) {

        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }

        return value;
    }

    private static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(field + " is required");
        }

        return value.trim();
    }
}
