package io.academicmonitor.academic.application.port;

import java.util.Collection;

public interface AcademicPlatformPort {

    AcademicPlatformSnapshot fetchSnapshot(AcademicPlatformContext context);

    default AcademicPlatformSnapshot fetchSnapshot(AcademicPlatformContext context, AcademicPlatformFilter filter) {

        return fetchSnapshot(context);
    }

    default PlatformGuardianSyncSnapshot fetchGuardians(
            AcademicPlatformContext context, Collection<String> studentExternalIds) {
        return PlatformGuardianSyncSnapshot.empty();
    }
}
