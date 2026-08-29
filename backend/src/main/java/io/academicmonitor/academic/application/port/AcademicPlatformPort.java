package io.academicmonitor.academic.application.port;

public interface AcademicPlatformPort {

    AcademicPlatformSnapshot fetchSnapshot(
        AcademicPlatformContext context);

    default AcademicPlatformSnapshot fetchSnapshot(
        AcademicPlatformContext context,
        AcademicPlatformFilter filter) {

        return fetchSnapshot(context);
    }
}
