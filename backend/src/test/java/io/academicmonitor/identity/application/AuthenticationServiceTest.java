package io.academicmonitor.identity.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.academicmonitor.identity.config.AuthProperties;
import io.academicmonitor.identity.config.AuthSecurityConfiguration;
import io.academicmonitor.identity.domain.RefreshToken;
import io.academicmonitor.identity.domain.RefreshTokenRepository;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");
    private final UserRepository users = mock(UserRepository.class);
    private final InstitutionRepository institutions = mock(InstitutionRepository.class);
    private final InstitutionMembershipRepository memberships = mock(InstitutionMembershipRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final AccessTokenService access = mock(AccessTokenService.class);
    private final RefreshTokenGenerator generator = new RefreshTokenGenerator();
    private final PasswordEncoder passwords = new AuthSecurityConfiguration().passwordEncoder();
    private User user;
    private Institution institution;
    private InstitutionMembership membership;
    private AuthenticationService service;

    @BeforeEach
    void setup() {
        user = new User("teacher@example.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setPasswordHash(passwords.encode("test-password"));
        institution = institution("School A");
        membership = new InstitutionMembership(user.getId(), institution.getId(), InstitutionRole.TEACHER);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(memberships.findByUserId(user.getId())).thenReturn(List.of(membership));
        when(memberships.findByUserIdAndInstitutionId(user.getId(), institution.getId()))
                .thenReturn(Optional.of(membership));
        when(institutions.findById(institution.getId())).thenReturn(Optional.of(institution));
        when(tokens.save(any())).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                ReflectionTestUtils.setField(token, "id", UUID.randomUUID());
            }
            return token;
        });
        when(access.issue(any())).thenReturn("unit-test-access-token");
        service = new AuthenticationService(
                users,
                institutions,
                memberships,
                tokens,
                passwords,
                generator,
                access,
                new AuthProperties(Duration.ofMinutes(15), Duration.ofDays(7), null, false, "Lax"),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void logsInNormalizesEmailAndStoresOnlyAnArgon2idPasswordAndRefreshDigest() {
        var session = service.login("  TEACHER@EXAMPLE.COM ", "test-password", null);
        assertEquals(user.getId(), session.view().user().id());
        assertEquals(institution.getId(), session.view().institution().id());
        assertTrue(user.getPasswordHash().startsWith("$argon2id$"));
        assertTrue(passwords.matches("test-password", user.getPasswordHash()));
        assertFalse(passwords.matches("wrong", user.getPasswordHash()));
        assertEquals(32, java.util.Base64.getUrlDecoder().decode(session.refreshToken()).length);
        verify(tokens)
                .save(argThat(token -> token.getTokenHash().equals(generator.hash(session.refreshToken()))
                        && !token.getTokenHash().equals(session.refreshToken())
                        && token.getExpiresAt().equals(NOW.plus(Duration.ofDays(7)))));
    }

    @Test
    void invalidPasswordAndMissingEmailHaveIdenticalFailure() {
        var invalid = assertThrows(AuthFailure.class, () -> service.login(user.getEmail(), "wrong", null));
        var missing = assertThrows(AuthFailure.class, () -> service.login("missing@example.com", "wrong", null));
        assertEquals("Correo o contraseña incorrectos.", invalid.getMessage());
        assertEquals(invalid.getMessage(), missing.getMessage());
        verify(tokens, never()).save(any());
    }

    @Test
    void deniesMissingPasswordHash() {
        ReflectionTestUtils.setField(user, "passwordHash", null);
        assertThrows(AuthFailure.class, () -> service.login(user.getEmail(), "test-password", null));
    }

    @Test
    void deniesInactiveUser() {
        user.deactivate();
        assertThrows(AuthFailure.class, () -> service.login(user.getEmail(), "test-password", null));
    }

    @Test
    void deniesInactiveMembershipAndInstitution() {
        membership.deactivate();
        assertThrows(AuthFailure.class, () -> service.login(user.getEmail(), "test-password", null));
        membership.activate();
        institution.deactivate();
        assertThrows(AuthFailure.class, () -> service.login(user.getEmail(), "test-password", null));
    }

    @Test
    void requiresSelectionForMultipleActiveInstitutionsAndValidatesSuppliedInstitution() {
        Institution second = institution("School B");
        var other = new InstitutionMembership(user.getId(), second.getId(), InstitutionRole.ADMIN);
        when(institutions.findById(second.getId())).thenReturn(Optional.of(second));
        when(memberships.findByUserId(user.getId())).thenReturn(List.of(membership, other));
        var error = assertThrows(
                InstitutionSelectionRequired.class, () -> service.login(user.getEmail(), "test-password", null));
        assertEquals(2, error.institutions().size());
        verify(tokens, never()).save(any());
        assertEquals(
                second.getId(),
                service.login(user.getEmail(), "test-password", second.getId())
                        .view()
                        .institution()
                        .id());
        assertThrows(AuthFailure.class, () -> service.login(user.getEmail(), "test-password", UUID.randomUUID()));
    }

    @Test
    void rotatesRefreshAndRejectsThePreviouslyConsumedToken() {
        String raw = generator.generate();
        RefreshToken previous = refresh(raw, NOW.plusSeconds(3600));
        var next = service.refresh(raw);
        assertNotEquals(raw, next.refreshToken());
        assertEquals(NOW, previous.getRevokedAt());
        assertEquals(next.refreshTokenId(), previous.getReplacedByTokenId());
        assertThrows(AuthFailure.class, () -> service.refresh(raw));
        verify(tokens)
                .save(argThat(token -> token != previous && token.getFamilyId().equals(previous.getFamilyId())));
    }

    @Test
    void rejectsExpiredRevokedUnknownAndMissingRefresh() {
        String expired = generator.generate();
        refresh(expired, NOW);
        assertThrows(AuthFailure.class, () -> service.refresh(expired));
        String revoked = generator.generate();
        refresh(revoked, NOW.plusSeconds(3600)).revoke(NOW);
        assertThrows(AuthFailure.class, () -> service.refresh(revoked));
        assertThrows(AuthFailure.class, () -> service.refresh(generator.generate()));
        assertThrows(AuthFailure.class, () -> service.refresh(null));
    }

    @Test
    void refreshRevalidatesUserMembershipAndInstitutionActivity() {
        String raw = generator.generate();
        refresh(raw, NOW.plusSeconds(3600));
        user.deactivate();
        assertThrows(AuthFailure.class, () -> service.refresh(raw));
        user.activate();
        membership.deactivate();
        assertThrows(AuthFailure.class, () -> service.refresh(raw));
        membership.activate();
        institution.deactivate();
        assertThrows(AuthFailure.class, () -> service.refresh(raw));
        verify(access, never()).issue(any());
    }

    @Test
    void logoutRevokesOnlyThePresentedSession() {
        String raw = generator.generate();
        RefreshToken token = refresh(raw, NOW.plusSeconds(3600));
        service.logout(raw);
        assertEquals(NOW, token.getRevokedAt());
        assertThrows(AuthFailure.class, () -> service.refresh(raw));
        verify(tokens).save(token);
    }

    @Test
    void tokenGeneratorUsesFreshRandomnessAndStableSha256Digests() {
        String one = generator.generate();
        assertNotEquals(one, generator.generate());
        assertEquals(64, generator.hash(one).length());
        assertEquals(generator.hash(one), generator.hash(one));
    }

    private RefreshToken refresh(String raw, Instant expires) {
        var token = new RefreshToken(
                user.getId(),
                institution.getId(),
                generator.hash(raw),
                UUID.randomUUID(),
                NOW.minusSeconds(1),
                expires);
        ReflectionTestUtils.setField(token, "id", UUID.randomUUID());
        when(tokens.lockByTokenHash(generator.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    private Institution institution(String name) {
        var result = new Institution(name, "America/Guayaquil");
        ReflectionTestUtils.setField(result, "id", UUID.randomUUID());
        return result;
    }
}
