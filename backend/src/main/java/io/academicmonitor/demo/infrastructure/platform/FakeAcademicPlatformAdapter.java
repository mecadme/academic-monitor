package io.academicmonitor.demo.infrastructure.platform;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicYearSnapshot;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.academic.application.port.PlatformGradeSnapshot;
import io.academicmonitor.academic.application.port.PlatformStudentSnapshot;
import io.academicmonitor.demo.application.DemoScenario;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class FakeAcademicPlatformAdapter implements AcademicPlatformPort {

    private static final String COURSE_EXTERNAL_ID = "physics-1bgu-a";
    private static final String ACTIVITY_EXTERNAL_ID = "activity-mru-001";
    private static final String PERIOD_EXTERNAL_ID = "period-2026-2027-1";

    private final DemoScenario scenario;

    FakeAcademicPlatformAdapter(DemoScenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public AcademicPlatformSnapshot fetchSnapshot(AcademicPlatformContext context) {

        List<PlatformStudentSnapshot> students = List.of(
                new PlatformStudentSnapshot("student-001", "Ana", "Torres"),
                new PlatformStudentSnapshot("student-002", "Carlos", "Vega"),
                new PlatformStudentSnapshot("student-003", "Sofía", "López"),
                new PlatformStudentSnapshot("student-004", "Mateo", "Cárdenas"));

        Instant recordedAt = Instant.now();

        List<PlatformGradeSnapshot> grades = List.of(
                grade("student-001", "9.20", "9.20", recordedAt),
                grade("student-002", "7.00", "7.00", recordedAt),
                grade("student-003", "6.40", "8.10", recordedAt),
                grade("student-004", "4.80", "8.50", recordedAt));

        PlatformActivitySnapshot activity = new PlatformActivitySnapshot(
                ACTIVITY_EXTERNAL_ID,
                "Movimiento rectilíneo",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 9, 25),
                PERIOD_EXTERNAL_ID,
                grades);

        PlatformAcademicPeriodSnapshot period =
                new PlatformAcademicPeriodSnapshot(PERIOD_EXTERNAL_ID, "Primer período", "P1", 1);

        PlatformAcademicYearSnapshot academicYear = new PlatformAcademicYearSnapshot(
                "academic-year-2026-2027",
                "Año lectivo 2026-2027",
                "2026-2027",
                new BigDecimal("10.00"),
                List.of(period));

        PlatformCourseSnapshot course = new PlatformCourseSnapshot(
                COURSE_EXTERNAL_ID, "1.º BGU A", "Física", academicYear, List.of(activity), students);

        return new AcademicPlatformSnapshot(List.of(course));
    }

    private PlatformGradeSnapshot grade(
            String studentExternalId, String initialScore, String improvedScore, Instant recordedAt) {

        String score = scenario == DemoScenario.IMPROVED ? improvedScore : initialScore;

        return new PlatformGradeSnapshot(studentExternalId, new BigDecimal(score), recordedAt);
    }
}
