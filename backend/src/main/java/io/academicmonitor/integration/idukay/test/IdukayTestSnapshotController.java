package io.academicmonitor.integration.idukay.test;

import io.academicmonitor.academic.application.port.AcademicPlatformContext;
import io.academicmonitor.academic.application.port.AcademicPlatformSnapshot;
import io.academicmonitor.integration.idukay.IdukayAcademicPlatformAdapter;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/idukay")
@ConditionalOnProperty(
    prefix = "app.idukay",
    name = "test-login-enabled",
    havingValue = "true")
public class IdukayTestSnapshotController {

    private final IdukayAcademicPlatformAdapter adapter;

    public IdukayTestSnapshotController(
        IdukayAcademicPlatformAdapter adapter) {

        this.adapter = adapter;
    }

    @GetMapping("/test-snapshot")
    public TestSnapshotResponse testSnapshot(
        @RequestParam UUID institutionId,
        @RequestParam UUID teacherUserId) {

        AcademicPlatformContext context =
            new AcademicPlatformContext(
                institutionId,
                teacherUserId);

        AcademicPlatformSnapshot snapshot =
            adapter.fetchSnapshot(context);

        int courses = snapshot.courses().size();

        int students = snapshot.courses()
            .stream()
            .mapToInt(course -> course.students().size())
            .sum();

        int activities = snapshot.courses()
            .stream()
            .mapToInt(course -> course.activities().size())
            .sum();

        int grades = snapshot.courses()
            .stream()
            .flatMap(course -> course.activities().stream())
            .mapToInt(activity -> activity.grades().size())
            .sum();

        return new TestSnapshotResponse(
            "OK",
            courses,
            students,
            activities,
            grades);
    }

    public record TestSnapshotResponse(
        String status,
        int courses,
        int students,
        int activities,
        int grades) {}
}
