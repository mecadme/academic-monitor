package io.academicmonitor.demo.infrastructure.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.demo.application.DemoScenario;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FakeAcademicPlatformAdapterTest {

    private static final AcademicPlatformContext CONTEXT = new AcademicPlatformContext(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Test
    void providesNeutralAcademicCalendarWithoutChangingDemoScenarios() {
        AcademicPlatformSnapshot initial = new FakeAcademicPlatformAdapter(DemoScenario.INITIAL).fetchSnapshot(CONTEXT);
        AcademicPlatformSnapshot improved =
                new FakeAcademicPlatformAdapter(DemoScenario.IMPROVED).fetchSnapshot(CONTEXT);

        var initialCourse = initial.courses().getFirst();
        var initialActivity = initialCourse.activities().getFirst();

        assertNotNull(initialCourse.academicYear());
        assertEquals("academic-year-2026-2027", initialCourse.academicYear().externalId());
        assertEquals(
                "period-2026-2027-1",
                initialCourse.academicYear().periods().getFirst().externalId());
        assertEquals("period-2026-2027-1", initialActivity.periodExternalId());

        assertEquals(new BigDecimal("4.80"), initialActivity.grades().get(3).score());
        assertEquals(
                new BigDecimal("8.50"),
                improved.courses()
                        .getFirst()
                        .activities()
                        .getFirst()
                        .grades()
                        .get(3)
                        .score());
    }
}
