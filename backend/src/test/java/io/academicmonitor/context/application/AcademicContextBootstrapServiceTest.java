package io.academicmonitor.context.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.academicmonitor.context.config.AcademicContextProperties;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AcademicContextBootstrapServiceTest {
    private static final String EMAIL = "local.teacher@academicmonitor.local";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();

    @Mock
    private UserRepository users;

    @Mock
    private InstitutionRepository institutions;

    @Mock
    private InstitutionMembershipRepository memberships;

    @Mock
    private PasswordEncoder passwords;

    private AcademicContextBootstrapService service;

    @BeforeEach
    void setUp() {
        service = serviceWithPassword("");
    }

    @Test
    void createsFirstIdentityAndTeacherMembershipWithoutInventingPassword() {
        User user = persistedUser();
        Institution institution = mock(Institution.class);
        when(institution.getId()).thenReturn(INSTITUTION_ID);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(users.save(any())).thenReturn(user);
        when(institutions.save(any())).thenReturn(institution);
        when(memberships.findByUserId(USER_ID)).thenReturn(List.of());
        service.bootstrap();
        var newUser = ArgumentCaptor.forClass(User.class);
        verify(users).save(newUser.capture());
        assertEquals(EMAIL, newUser.getValue().getEmail());
        var membership = ArgumentCaptor.forClass(InstitutionMembership.class);
        verify(memberships).save(membership.capture());
        assertEquals(USER_ID, membership.getValue().getUserId());
        assertEquals(INSTITUTION_ID, membership.getValue().getInstitutionId());
        assertEquals(InstitutionRole.TEACHER, membership.getValue().getInstitutionRole());
        verifyNoInteractions(passwords);
        verify(user, never()).setPasswordHash(any());
    }

    @Test
    void existingIdentityAndMembershipArePreservedAcrossRepeatedStartup() {
        User user = existingUser();
        service.bootstrap();
        service.bootstrap();
        verify(users, never()).save(any());
        verify(memberships, never()).save(any());
        verifyNoInteractions(institutions, passwords);
        verify(user, never()).setPasswordHash(any());
    }

    @Test
    void initializesOnlyMissingPasswordOnExistingIdentity() {
        User user = existingUser();
        when(passwords.encode("test-only-password")).thenReturn("$argon2id$test-hash");
        service = serviceWithPassword("test-only-password");
        service.bootstrap();
        verify(user).setPasswordHash("$argon2id$test-hash");
        verify(users).save(user);
        verify(memberships, never()).save(any());
        verifyNoInteractions(institutions);
    }

    @Test
    void neverRewritesExistingPassword() {
        User user = existingUser();
        when(user.getPasswordHash()).thenReturn("$argon2id$existing");
        service = serviceWithPassword("test-only-password");
        service.bootstrap();
        verifyNoInteractions(passwords, institutions);
        verify(user, never()).setPasswordHash(any());
        verify(users, never()).save(any());
    }

    @Test
    void multipleMembershipsDoNotSelectAnInstitutionOrPreventStartup() {
        User user = persistedUser();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(memberships.findByUserId(USER_ID))
                .thenReturn(List.of(
                        new InstitutionMembership(USER_ID, INSTITUTION_ID, InstitutionRole.TEACHER),
                        new InstitutionMembership(USER_ID, UUID.randomUUID(), InstitutionRole.TEACHER)));
        service.bootstrap();
        verifyNoInteractions(institutions, passwords);
        verify(memberships, never()).save(any());
    }

    @Test
    void inactiveMembershipIsNotReplacedWithNewInstitution() {
        User user = persistedUser();
        var inactive = new InstitutionMembership(USER_ID, INSTITUTION_ID, InstitutionRole.TEACHER);
        inactive.deactivate();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(memberships.findByUserId(USER_ID)).thenReturn(List.of(inactive));
        service.bootstrap();
        verifyNoInteractions(institutions, passwords);
        verify(memberships, never()).save(any());
    }

    @Test
    void inactiveUserRemainsInactiveWithoutPreventingOtherUsersFromLoggingIn() {
        User inactive = new User(EMAIL);
        inactive.deactivate();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(inactive));
        service.bootstrap();
        verifyNoInteractions(passwords, memberships, institutions);
        verify(users, never()).save(any());
    }

    private User existingUser() {
        User user = persistedUser();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(memberships.findByUserId(USER_ID))
                .thenReturn(List.of(new InstitutionMembership(USER_ID, INSTITUTION_ID, InstitutionRole.TEACHER)));
        return user;
    }

    private User persistedUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.isActive()).thenReturn(true);
        return user;
    }

    private AcademicContextBootstrapService serviceWithPassword(String password) {
        return new AcademicContextBootstrapService(
                users,
                institutions,
                memberships,
                new AcademicContextProperties(EMAIL, "Academic Monitor Local", "America/Guayaquil", password),
                passwords);
    }
}
