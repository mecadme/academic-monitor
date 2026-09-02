package io.academicmonitor.academic.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AcademicPeriodCatalogQueryServiceIT extends PostgresIntegrationTest {

    @Autowired
    private AcademicPeriodCatalogQueryService service;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private AcademicCourseRepository courseRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void exposesOnlyTeacherScopedPeriodsAndDetectsSynchronizationFromScopedActivities() {
        Institution institution = institutionRepository.save(new Institution("Period School", "America/Guayaquil"));
        Institution otherInstitution =
                institutionRepository.save(new Institution("Other Period School", "America/Guayaquil"));
        User teacher = userRepository.save(new User("period.teacher@example.com"));
        User otherTeacher = userRepository.save(new User("other.period.teacher@example.com"));
        AcademicYear scopedYear = saveYear(institution, "scoped-year", "2025 - 2026");
        AcademicYear otherTeacherYear = saveYear(institution, "other-teacher-year", "2024 - 2025");
        AcademicYear otherInstitutionYear = saveYear(otherInstitution, "other-institution-year", "2025 - 2026");
        AcademicPeriod t2 = academicPeriodRepository.save(
                new AcademicPeriod(scopedYear.getId(), "scoped-t2", "Segundo trimestre", "T2", 2));
        AcademicPeriod t1 = academicPeriodRepository.save(
                new AcademicPeriod(scopedYear.getId(), "scoped-t1", "Primer trimestre", "T1", 1));
        academicPeriodRepository.save(
                new AcademicPeriod(otherTeacherYear.getId(), "other-teacher-period", "Other teacher period", "OTP", 1));
        academicPeriodRepository.save(new AcademicPeriod(
                otherInstitutionYear.getId(), "other-institution-period", "Other institution period", "OIP", 1));
        AcademicCourse scopedCourse = saveCourse(institution, teacher, scopedYear, "scoped-course");
        AcademicCourse otherTeacherCourse =
                saveCourse(institution, otherTeacher, otherTeacherYear, "other-teacher-course");
        saveCourse(otherInstitution, teacher, otherInstitutionYear, "other-institution-course");
        activityRepository.save(new Activity(
                scopedCourse.getId(),
                t2.getId(),
                "TEST",
                "scoped-activity",
                "Scoped activity",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 1, 15)));
        activityRepository.save(new Activity(
                otherTeacherCourse.getId(),
                t1.getId(),
                "TEST",
                "foreign-activity",
                "Foreign activity",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 1, 15)));

        entityManager.flush();
        entityManager.clear();

        AcademicPeriodCatalogResponse result = service.getPeriods(institution.getId(), teacher.getId());

        assertEquals(
                List.of(t1.getId(), t2.getId()),
                result.periods().stream()
                        .map(AcademicPeriodCatalogResponse.AcademicPeriodItem::id)
                        .toList());
        assertFalse(result.periods().getFirst().synchronizedPeriod());
        assertTrue(result.periods().get(1).synchronizedPeriod());
    }

    private AcademicYear saveYear(Institution institution, String externalId, String name) {
        return academicYearRepository.save(
                new AcademicYear(institution.getId(), "TEST", externalId, name, name, new BigDecimal("10.00")));
    }

    private AcademicCourse saveCourse(
            Institution institution, User teacher, AcademicYear academicYear, String externalId) {
        return courseRepository.save(new AcademicCourse(
                institution.getId(),
                teacher.getId(),
                academicYear.getId(),
                "TEST",
                externalId,
                "Course " + externalId,
                "Subject"));
    }
}
