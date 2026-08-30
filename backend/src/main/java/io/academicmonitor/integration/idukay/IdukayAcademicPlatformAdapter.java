package io.academicmonitor.integration.idukay;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformFilter;
import io.academicmonitor.academic.application.port.AcademicPlatformPort;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.academic.application.port.PlatformActivitySnapshot;
import io.academicmonitor.academic.application.port.PlatformCourseSnapshot;
import io.academicmonitor.integration.idukay.activity.IdukayActivityMapper;
import io.academicmonitor.integration.idukay.activity.IdukayCourseActivitiesClient;
import io.academicmonitor.integration.idukay.auth.IdukayAuthenticatedSession;
import io.academicmonitor.integration.idukay.auth.IdukaySessionProvider;
import io.academicmonitor.integration.idukay.course.IdukayCourseMapper;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCourseDto;
import io.academicmonitor.integration.idukay.course.IdukayTeacherCoursesClient;
import io.academicmonitor.integration.idukay.period.IdukayCoursePeriodClient;
import io.academicmonitor.integration.idukay.period.IdukayPeriodResolver;
import java.util.List;
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

        var customYear = coursePeriodClient.findCustomYear(session, course.id());

        var idukayActivities = activitiesClient.findActivities(session, course.id());

        if (filter.hasPeriod()) {

            idukayActivities = idukayActivities.stream()
                    .filter(activity -> IdukayPeriodResolver.findTermByPartId(customYear, activity.partId())
                            .map(term -> filter.periodExternalId().equals(term.id()))
                            .orElse(false))
                    .toList();
        }

        List<PlatformActivitySnapshot> activities = idukayActivities.stream()
                .map(activity -> IdukayActivityMapper.toSnapshot(activity, customYear.baseScore()))
                .toList();

        return new PlatformCourseSnapshot(base.externalId(), base.name(), base.subject(), activities, base.students());
    }
}
