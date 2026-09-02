package io.academicmonitor.integration.idukay.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.academicmonitor.academic.application.AcademicBatchSyncResult;
import io.academicmonitor.academic.application.AcademicSyncResult;
import io.academicmonitor.academic.application.AcademicSyncService;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.integration.idukay.IdukayAcademicPlatformAdapter;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdukayTestSnapshotControllerTest {

    @Test
    void filteredSyncResponseContainsTheMappedInternalAcademicPeriodId() {
        UUID institutionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID teacherId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID courseId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID internalPeriodId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        IdukayAcademicPlatformAdapter adapter = mock(IdukayAcademicPlatformAdapter.class);
        AcademicSyncService syncService = mock(AcademicSyncService.class);
        IdukayTestSnapshotController controller = new IdukayTestSnapshotController(
                adapter,
                syncService,
                mock(IdukaySessionProvider.class),
                mock(IdukayTeacherCoursesClient.class),
                mock(IdukayCoursePeriodClient.class));
        AcademicPlatformFilter filter = new AcademicPlatformFilter("external-period-t2");
        AcademicBatchSyncResult syncResult = new AcademicBatchSyncResult(
                List.of(new AcademicSyncResult(courseId, "Course", 20, 100, 12, 8, 4, internalPeriodId)));
        when(syncService.synchronizeAll(institutionId, teacherId, "IDUKAY", adapter, filter))
                .thenReturn(syncResult);

        IdukayTestSnapshotController.TestBatchSyncResponse response =
                controller.testSync(institutionId, teacherId, "external-period-t2");

        assertEquals(internalPeriodId, response.academicPeriodId());
        assertEquals(1, response.coursesProcessed());
        assertEquals(100, response.gradesProcessed());
        assertEquals(12, response.openAlerts());
    }
}
