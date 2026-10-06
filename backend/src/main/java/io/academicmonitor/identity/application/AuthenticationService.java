package io.academicmonitor.identity.application;

import io.academicmonitor.identity.application.InstitutionSelectionRequired.InstitutionChoice;
import io.academicmonitor.identity.config.AuthProperties;
import io.academicmonitor.identity.domain.RefreshToken;
import io.academicmonitor.identity.domain.RefreshTokenRepository;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final InstitutionMembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final RefreshTokenGenerator generator;
    private final AccessTokenService accessTokens;
    private final AuthProperties properties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserRepository users,
            InstitutionRepository institutions,
            InstitutionMembershipRepository memberships,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwords,
            RefreshTokenGenerator generator,
            AccessTokenService accessTokens,
            AuthProperties properties,
            Clock clock) {
        this.users = users;
        this.institutions = institutions;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.generator = generator;
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwords.encode("invalid-login-timing-placeholder");
    }

    @Transactional
    public AuthSession login(String email, String password, UUID institutionId) {
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        String hash = user == null || user.getPasswordHash() == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwords.matches(password, hash);
        if (user == null || !matches || !user.isActive() || user.getPasswordHash() == null) {
            throw new AuthFailure();
        }
        List<SelectedMembership> active = memberships.findByUserId(user.getId()).stream()
                .filter(InstitutionMembership::isActive)
                .flatMap(membership ->
                        institutions.findById(membership.getInstitutionId()).filter(Institution::isActive).stream()
                                .map(institution -> new SelectedMembership(membership, institution)))
                .toList();
        SelectedMembership selected;
        if (institutionId != null) {
            selected = active.stream()
                    .filter(item -> institutionId.equals(item.institution().getId()))
                    .findFirst()
                    .orElseThrow(AuthFailure::new);
        } else if (active.size() == 1) {
            selected = active.getFirst();
        } else if (active.size() > 1) {
            throw new InstitutionSelectionRequired(active.stream()
                    .map(item -> new InstitutionChoice(
                            item.institution().getId(),
                            item.institution().getName(),
                            item.membership().getInstitutionRole()))
                    .toList());
        } else {
            throw new AuthFailure();
        }
        return createSession(user, selected, UUID.randomUUID());
    }

    @Transactional
    public AuthSession refresh(String rawToken) {
        // The row lock is held until the replacement and revocation commit atomically.
        RefreshToken previous =
                refreshTokens.lockByTokenHash(generator.hash(rawToken)).orElseThrow(AuthFailure::new);
        if (!previous.isUsableAt(clock.instant())) {
            throw new AuthFailure();
        }
        User user = users.findById(previous.getUserId()).filter(User::isActive).orElseThrow(AuthFailure::new);
        SelectedMembership selected = activeMembership(user.getId(), previous.getInstitutionId());
        AuthSession next = createSession(user, selected, previous.getFamilyId());
        previous.replaceWith(next.refreshTokenId(), clock.instant());
        refreshTokens.save(previous);
        return next;
    }

    @Transactional
    public void logout(String rawToken) {
        RefreshToken token =
                refreshTokens.lockByTokenHash(generator.hash(rawToken)).orElseThrow(AuthFailure::new);
        token.revoke(clock.instant());
        refreshTokens.save(token);
    }

    @Transactional(readOnly = true)
    public AuthView me(AcademicMonitorPrincipal principal) {
        User user = users.findById(principal.userId()).filter(User::isActive).orElseThrow(AuthFailure::new);
        return view(user, activeMembership(user.getId(), principal.institutionId()));
    }

    private SelectedMembership activeMembership(UUID userId, UUID institutionId) {
        InstitutionMembership membership = memberships
                .findByUserIdAndInstitutionId(userId, institutionId)
                .filter(InstitutionMembership::isActive)
                .orElseThrow(AuthFailure::new);
        Institution institution = institutions
                .findById(institutionId)
                .filter(Institution::isActive)
                .orElseThrow(AuthFailure::new);
        return new SelectedMembership(membership, institution);
    }

    private AuthSession createSession(User user, SelectedMembership selected, UUID familyId) {
        String rawToken = generator.generate();
        Instant now = clock.instant();
        RefreshToken persisted = refreshTokens.save(new RefreshToken(
                user.getId(),
                selected.institution().getId(),
                generator.hash(rawToken),
                familyId,
                now,
                now.plus(properties.refreshTtl())));
        String jwt = accessTokens.issue(new AcademicMonitorPrincipal(
                user.getId(),
                selected.institution().getId(),
                user.getSystemRole(),
                selected.membership().getInstitutionRole()));
        return new AuthSession(view(user, selected), jwt, rawToken, persisted.getId());
    }

    private AuthView view(User user, SelectedMembership selected) {
        return new AuthView(
                new AuthView.UserView(user.getId(), user.getEmail(), user.getSystemRole()),
                new AuthView.InstitutionView(
                        selected.institution().getId(),
                        selected.institution().getName(),
                        selected.membership().getInstitutionRole()));
    }

    private record SelectedMembership(InstitutionMembership membership, Institution institution) {}

    public record AuthSession(AuthView view, String accessToken, String refreshToken, UUID refreshTokenId) {
        @Override
        public String toString() {
            return "AuthSession[credentials redacted]";
        }
    }
}
