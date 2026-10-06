package io.academicmonitor.identity.infrastructure.persistence;

import io.academicmonitor.identity.domain.RefreshToken;
import io.academicmonitor.identity.domain.RefreshTokenRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final RefreshTokenDataRepository repository;

    RefreshTokenRepositoryAdapter(RefreshTokenDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return repository.saveAndFlush(token);
    }

    @Override
    public Optional<RefreshToken> lockByTokenHash(String hash) {
        return repository.lockByTokenHash(hash);
    }
}
