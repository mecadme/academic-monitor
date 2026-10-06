package io.academicmonitor.identity.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import io.academicmonitor.identity.application.AuthFailure;
import io.academicmonitor.identity.application.AuthenticationService;
import io.academicmonitor.identity.application.RefreshTokenGenerator;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthenticationPersistenceIT extends PostgresIntegrationTest {
    @Autowired
    private AuthenticationService authentication;

    @Autowired
    private UserRepository users;

    @Autowired
    private InstitutionRepository institutions;

    @Autowired
    private InstitutionMembershipRepository memberships;

    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private RefreshTokenGenerator generator;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User user;
    private Institution institution;

    @BeforeEach
    void createFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            user = new User("auth-it-" + UUID.randomUUID() + "@example.com");
            user.setPasswordHash(passwords.encode("test-password-only"));
            user = users.save(user);
            institution = institutions.save(new Institution("Auth integration", "America/Guayaquil"));
            memberships.save(new InstitutionMembership(user.getId(), institution.getId(), InstitutionRole.TEACHER));
        });
    }

    @Test
    void flywayV9AndHibernatePersistOnlyDigestAndRotateWithReplacementForeignKey() {
        var first = authentication.login(user.getEmail(), "test-password-only", null);
        String digest = jdbc.queryForObject(
                "select token_hash from auth_refresh_tokens where id = ?", String.class, first.refreshTokenId());
        assertEquals(generator.hash(first.refreshToken()), digest);
        assertNotEquals(first.refreshToken(), digest);
        assertEquals(7, first.refreshTokenId().version());
        assertEquals(user.getId(), first.view().user().id());
        var next = authentication.refresh(first.refreshToken());
        assertNotEquals(first.refreshToken(), next.refreshToken());
        assertEquals(
                next.refreshTokenId(),
                jdbc.queryForObject(
                        "select replaced_by_token_id from auth_refresh_tokens where id = ?",
                        UUID.class,
                        first.refreshTokenId()));
        assertNotNull(jdbc.queryForObject(
                "select revoked_at from auth_refresh_tokens where id = ?",
                java.sql.Timestamp.class,
                first.refreshTokenId()));
        assertThrows(AuthFailure.class, () -> authentication.refresh(first.refreshToken()));
        authentication.logout(next.refreshToken());
        assertThrows(AuthFailure.class, () -> authentication.refresh(next.refreshToken()));
        assertEquals(
                "9",
                jdbc.queryForObject(
                        "select version from flyway_schema_history where success and version = '9'", String.class));
    }

    @Test
    void onlyOneOfTwoSimultaneousRefreshesConsumesTheSameToken() throws Exception {
        var initial = authentication.login(user.getEmail(), "test-password-only", null);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempts = new ArrayList<Future<Boolean>>();
            for (int index = 0; index < 2; index++) {
                attempts.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Refresh race did not start");
                    }
                    try {
                        authentication.refresh(initial.refreshToken());
                        return true;
                    } catch (AuthFailure denied) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int successes = 0;
            for (var attempt : attempts) {
                if (attempt.get(20, TimeUnit.SECONDS)) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        }
        assertEquals(
                2,
                jdbc.queryForObject(
                        "select count(*) from auth_refresh_tokens where user_id = ?", Integer.class, user.getId()));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from auth_refresh_tokens where user_id = ? and revoked_at is null",
                        Integer.class,
                        user.getId()));
    }

    @Test
    void refreshRechecksDatabaseActivityAndExpiry() {
        var first = authentication.login(user.getEmail(), "test-password-only", null);
        jdbc.update("update users set active = false where id = ?", user.getId());
        assertThrows(AuthFailure.class, () -> authentication.refresh(first.refreshToken()));
        jdbc.update("update users set active = true where id = ?", user.getId());
        jdbc.update("update institution_memberships set active = false where user_id = ?", user.getId());
        assertThrows(AuthFailure.class, () -> authentication.refresh(first.refreshToken()));
        jdbc.update("update institution_memberships set active = true where user_id = ?", user.getId());
        jdbc.update(
                "update auth_refresh_tokens set created_at = ?, expires_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(120)),
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)),
                first.refreshTokenId());
        assertThrows(AuthFailure.class, () -> authentication.refresh(first.refreshToken()));
    }

    @Test
    void logoutRevokesOnlyItsOwnDeviceSession() {
        var first = authentication.login(user.getEmail(), "test-password-only", null);
        var second = authentication.login(user.getEmail(), "test-password-only", null);
        authentication.logout(first.refreshToken());
        assertThrows(AuthFailure.class, () -> authentication.refresh(first.refreshToken()));
        assertNotNull(authentication.refresh(second.refreshToken()));
    }
}
