package io.academicmonitor.context.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.academicmonitor.context.config.AcademicContextProperties;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicContextBootstrapServiceTest {

    private static final String USER_EMAIL = "local.teacher@academicmonitor.local";
    private static final String INSTITUTION_NAME = "Academic Monitor Local";
    private static final String TIMEZONE = "America/Guayaquil";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private InstitutionMembershipRepository membershipRepository;

    private AcademicContextBootstrapService service;

    @BeforeEach
    void setUp() {
        AcademicContextProperties properties = new AcademicContextProperties(USER_EMAIL, INSTITUTION_NAME, TIMEZONE);

        service = new AcademicContextBootstrapService(
                userRepository, institutionRepository, membershipRepository, properties);
    }

    @Test
    void createsUserInstitutionAndTeacherMembershipOnFirstBootstrap() {
        User persistedUser = user(USER_ID);
        Institution persistedInstitution = institution(INSTITUTION_ID);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(institutionRepository.save(any(Institution.class))).thenReturn(persistedInstitution);

        AcademicContextResult result = service.bootstrap();

        assertEquals(new AcademicContextResult(INSTITUTION_ID, USER_ID), result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(USER_EMAIL, userCaptor.getValue().getEmail());

        ArgumentCaptor<Institution> institutionCaptor = ArgumentCaptor.forClass(Institution.class);
        verify(institutionRepository).save(institutionCaptor.capture());
        assertEquals(INSTITUTION_NAME, institutionCaptor.getValue().getName());
        assertEquals(TIMEZONE, institutionCaptor.getValue().getTimezone());

        ArgumentCaptor<InstitutionMembership> membershipCaptor = ArgumentCaptor.forClass(InstitutionMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertEquals(USER_ID, membershipCaptor.getValue().getUserId());
        assertEquals(INSTITUTION_ID, membershipCaptor.getValue().getInstitutionId());
        assertEquals(InstitutionRole.TEACHER, membershipCaptor.getValue().getInstitutionRole());
    }

    @Test
    void repeatedBootstrapReturnsSameContextWithoutCreatingDuplicates() {
        User persistedUser = user(USER_ID);
        Institution persistedInstitution = institution(INSTITUTION_ID);
        InstitutionMembership membership = membership(INSTITUTION_ID);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty(), Optional.of(persistedUser));
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(List.of(), List.of(membership));
        when(institutionRepository.save(any(Institution.class))).thenReturn(persistedInstitution);
        when(institutionRepository.findById(INSTITUTION_ID)).thenReturn(Optional.of(persistedInstitution));

        AcademicContextResult first = service.bootstrap();
        AcademicContextResult second = service.bootstrap();

        assertEquals(first, second);
        assertEquals(new AcademicContextResult(INSTITUTION_ID, USER_ID), second);
        verify(userRepository, times(1)).save(any(User.class));
        verify(institutionRepository, times(1)).save(any(Institution.class));
        verify(membershipRepository, times(1)).save(any(InstitutionMembership.class));
    }

    @Test
    void reusesExistingUserWithOneValidActiveMembership() {
        User persistedUser = user(USER_ID);
        Institution persistedInstitution = institution(INSTITUTION_ID);
        InstitutionMembership membership = membership(INSTITUTION_ID);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(persistedUser));
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membership));
        when(institutionRepository.findById(INSTITUTION_ID)).thenReturn(Optional.of(persistedInstitution));

        AcademicContextResult result = service.bootstrap();

        assertEquals(new AcademicContextResult(INSTITUTION_ID, USER_ID), result);
        verify(userRepository, never()).save(any(User.class));
        verify(institutionRepository, never()).save(any(Institution.class));
        verify(membershipRepository, never()).save(any(InstitutionMembership.class));
    }

    @Test
    void failsWhenMultipleActiveMembershipsMakeContextAmbiguous() {
        User persistedUser = user(USER_ID);
        InstitutionMembership firstMembership = membership(INSTITUTION_ID);
        InstitutionMembership secondMembership = membership(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(persistedUser));
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(List.of(firstMembership, secondMembership));

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::bootstrap);

        assertEquals("Bootstrap user has multiple active institution memberships", exception.getMessage());
        verifyNoInteractions(institutionRepository);
    }

    @Test
    void failsWhenActiveMembershipReferencesMissingInstitution() {
        User persistedUser = user(USER_ID);
        InstitutionMembership membership = membership(INSTITUTION_ID);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(persistedUser));
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membership));
        when(institutionRepository.findById(INSTITUTION_ID)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::bootstrap);

        assertEquals("Bootstrap user's active membership references a missing institution", exception.getMessage());
    }

    private User user(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private Institution institution(UUID id) {
        Institution institution = mock(Institution.class);
        when(institution.getId()).thenReturn(id);
        lenient().when(institution.isActive()).thenReturn(true);
        return institution;
    }

    private InstitutionMembership membership(UUID institutionId) {
        InstitutionMembership membership = mock(InstitutionMembership.class);
        when(membership.isActive()).thenReturn(true);
        lenient().when(membership.getInstitutionId()).thenReturn(institutionId);
        return membership;
    }
}
