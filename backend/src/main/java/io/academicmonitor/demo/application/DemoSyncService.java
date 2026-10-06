package io.academicmonitor.demo.application;

import io.academicmonitor.academic.application.AcademicSyncResult;
import io.academicmonitor.academic.application.AcademicSyncService;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.demo.infrastructure.platform.FakeAcademicPlatformAdapterFactory;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DemoSyncService {
    private static final String PLATFORM = "DEMO";
    private final AcademicSyncService academicSyncService;
    private final FakeAcademicPlatformAdapterFactory platformAdapterFactory;

    public DemoSyncService(
            AcademicSyncService academicSyncService, FakeAcademicPlatformAdapterFactory platformAdapterFactory) {
        this.academicSyncService = academicSyncService;
        this.platformAdapterFactory = platformAdapterFactory;
    }

    public DemoSyncResult sync(UUID institutionId, UUID teacherUserId, DemoScenario scenario) {
        AcademicPlatformPort platform = platformAdapterFactory.create(scenario);
        AcademicSyncResult sync = academicSyncService.synchronize(institutionId, teacherUserId, PLATFORM, platform);
        return new DemoSyncResult(
                institutionId,
                teacherUserId,
                sync.courseId(),
                sync.courseName(),
                scenario,
                sync.students(),
                sync.gradesProcessed(),
                sync.openAlerts(),
                sync.warnings(),
                sync.critical());
    }
}
