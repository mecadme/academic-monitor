package io.academicmonitor.integration.idukay;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicPeriodSnapshot;
import io.academicmonitor.academic.application.port.PlatformAcademicYearSnapshot;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.integration.idukay.activity.IdukayActivityDto;
import io.academicmonitor.integration.idukay.activity.IdukayActivityMapper;
import io.academicmonitor.integration.idukay.activity.IdukayCourseActivitiesClient;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.client.IdukayApiException;
import io.academicmonitor.integration.idukay.course.IdukayCourseMapper;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import io.academicmonitor.integration.idukay.period.IdukayCustomYearDto;
import io.academicmonitor.integration.idukay.period.IdukayPeriodResolver;
import io.academicmonitor.integration.idukay.period.IdukayTermDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class IdukayAcademicPlatformAdapter implements AcademicPlatformPort {

    private final IdukaySessionProvider sessionProvider;
    private final IdukayTeacherCoursesClient coursesClient;
    private final IdukayCourseActivitiesClient activitiesClient;
    private final IdukayCoursePeriodClient coursePeriodClient;

    public IdukayAcademicPlatformAdapter(
            IdukaySessionProvider sessionProvider,
            IdukayTeacherCoursesClient coursesClient,
            IdukayCourseActivitiesClient activitiesClient,
            IdukayCoursePeriodClient coursePeriodClient) {

        this.sessionProvider = sessionProvider;
        this.coursesClient = coursesClient;
        this.activitiesClient = activitiesClient;
        this.coursePeriodClient = coursePeriodClient;
    }

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
