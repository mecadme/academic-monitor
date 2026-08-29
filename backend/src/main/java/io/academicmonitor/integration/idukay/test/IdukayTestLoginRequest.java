package io.academicmonitor.integration.idukay.test;

import io.academicmonitor.integration.idukay.auth.IdukayFingerprint;
import java.util.UUID;

public record IdukayTestLoginRequest(
    String email,
    char[] password,
    String subdomainSchool,
    String schoolId,
    String profileId,
    UUID institutionId,
    UUID teacherUserId,
    IdukayFingerprint fingerprint) {}
