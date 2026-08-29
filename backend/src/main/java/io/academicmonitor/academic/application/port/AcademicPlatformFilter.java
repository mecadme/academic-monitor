package io.academicmonitor.academic.application.port;

public record AcademicPlatformFilter(
    String periodExternalId) {

    public static AcademicPlatformFilter all() {
        return new AcademicPlatformFilter(null);
    }

    public boolean hasPeriod() {
        return periodExternalId != null
            && !periodExternalId.isBlank();
    }
}
