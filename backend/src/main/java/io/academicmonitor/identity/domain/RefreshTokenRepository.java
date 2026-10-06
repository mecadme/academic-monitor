package io.academicmonitor.identity.domain;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> lockByTokenHash(String tokenHash);
}
