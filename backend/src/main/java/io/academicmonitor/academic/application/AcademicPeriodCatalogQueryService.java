package io.academicmonitor.academic.application;

import io.academicmonitor.academic.domain.AcademicCourse;
import io.academicmonitor.academic.domain.AcademicCourseRepository;
import io.academicmonitor.academic.domain.AcademicPeriod;
import io.academicmonitor.academic.domain.AcademicPeriodRepository;
import io.academicmonitor.academic.domain.AcademicYear;
import io.academicmonitor.academic.domain.AcademicYearRepository;
import io.academicmonitor.academic.domain.Activity;
import io.academicmonitor.academic.domain.ActivityRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicPeriodCatalogQueryService {

    private final AcademicCourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final ActivityRepository activityRepository;

    public AcademicPeriodCatalogQueryService(
            AcademicCourseRepository courseRepository,
            AcademicYearRepository academicYearRepository,
            AcademicPeriodRepository academicPeriodRepository,
            ActivityRepository activityRepository) {
        this.courseRepository = courseRepository;
        this.academicYearRepository = academicYearRepository;
        this.academicPeriodRepository = academicPeriodRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public AcademicPeriodCatalogResponse getPeriods(UUID institutionId, UUID teacherUserId) {
        Objects.requireNonNull(institutionId, "institutionId is required");
        Objects.requireNonNull(teacherUserId, "teacherUserId is required");

        List<AcademicCourse> courses =
                courseRepository.findByInstitutionIdAndTeacherUserId(institutionId, teacherUserId);

        Set<UUID> courseIds = courses.stream()
                .map(AcademicCourse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        Set<UUID> academicYearIds = courses.stream()
                .map(AcademicCourse::getAcademicYearId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (academicYearIds.isEmpty()) {
            return new AcademicPeriodCatalogResponse(institutionId, teacherUserId, List.of());
        }

        Map<UUID, AcademicYear> academicYearsById =
                academicYearRepository.findByInstitutionIdAndIdIn(institutionId, academicYearIds).stream()
                        .filter(year -> institutionId.equals(year.getInstitutionId()))
                        .filter(year -> academicYearIds.contains(year.getId()))
                        .collect(Collectors.toUnmodifiableMap(AcademicYear::getId, Function.identity()));

        if (academicYearsById.isEmpty()) {
            return new AcademicPeriodCatalogResponse(institutionId, teacherUserId, List.of());
        }

        Set<UUID> synchronizedPeriodIds = courseIds.isEmpty()
                ? Set.of()
                : activityRepository.findActivitiesByCourseIdIn(courseIds).stream()
                        .filter(activity -> courseIds.contains(activity.getCourseId()))
                        .map(Activity::getAcademicPeriodId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet());

        Comparator<AcademicPeriod> periodOrder = Comparator.comparing(
                        (AcademicPeriod period) -> academicYearsById
                                .get(period.getAcademicYearId())
                                .getName(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AcademicPeriod::getAcademicYearId)
                .thenComparingInt(AcademicPeriod::getOrder)
                .thenComparing(AcademicPeriod::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AcademicPeriod::getId);

        List<AcademicPeriodCatalogResponse.AcademicPeriodItem> periods =
                academicPeriodRepository.findByAcademicYearIdIn(academicYearsById.keySet()).stream()
                        .filter(period -> academicYearsById.containsKey(period.getAcademicYearId()))
                        .filter(period -> period.getId() != null)
                        .sorted(periodOrder)
                        .map(period -> new AcademicPeriodCatalogResponse.AcademicPeriodItem(
                                period.getId(),
                                period.getName(),
                                period.getAbbreviation(),
                                period.getOrder(),
                                synchronizedPeriodIds.contains(period.getId())))
                        .toList();

        return new AcademicPeriodCatalogResponse(institutionId, teacherUserId, periods);
    }
}
