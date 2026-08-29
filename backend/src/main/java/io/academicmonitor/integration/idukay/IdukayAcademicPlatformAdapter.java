package io.academicmonitor.integration.idukay;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
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
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IdukayAcademicPlatformAdapter
    implements AcademicPlatformPort {

    private final IdukaySessionProvider sessionProvider;
    private final IdukayTeacherCoursesClient coursesClient;
    private final IdukayCourseActivitiesClient activitiesClient;

    public IdukayAcademicPlatformAdapter(
        IdukaySessionProvider sessionProvider,
        IdukayTeacherCoursesClient coursesClient,
        IdukayCourseActivitiesClient activitiesClient) {

        this.sessionProvider = sessionProvider;
        this.coursesClient = coursesClient;
        this.activitiesClient = activitiesClient;
    }

    @Override
    public AcademicPlatformSnapshot fetchSnapshot(
        AcademicPlatformContext context) {

        IdukayAuthenticatedSession session =
            sessionProvider.getSession(context);

        List<PlatformCourseSnapshot> courses =
            coursesClient.findTeacherCourses(session)
                .stream()
                .map(course -> mapCourse(session, course))
                .toList();

        return new AcademicPlatformSnapshot(courses);
    }

    private PlatformCourseSnapshot mapCourse(
        IdukayAuthenticatedSession session,
        IdukayTeacherCourseDto course) {

        PlatformCourseSnapshot base =
            IdukayCourseMapper.toSnapshot(course);

        List<PlatformActivitySnapshot> activities =
            activitiesClient.findActivities(
                    session,
                    course.id())
                .stream()
                .map(IdukayActivityMapper::toSnapshot)
                .toList();

        return new PlatformCourseSnapshot(
            base.externalId(),
            base.name(),
            base.subject(),
            activities,
            base.students());
    }
}
