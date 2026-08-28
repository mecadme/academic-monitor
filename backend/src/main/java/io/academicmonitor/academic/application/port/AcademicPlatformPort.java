package io.academicmonitor.academic.application.port;

public interface AcademicPlatformPort {

    AcademicPlatformSnapshot fetchSnapshot(AcademicPlatformContext context);
}
