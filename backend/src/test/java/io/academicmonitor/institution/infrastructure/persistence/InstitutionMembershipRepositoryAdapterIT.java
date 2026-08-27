package io.academicmonitor.institution.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.*;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class InstitutionMembershipRepositoryAdapterIT extends PostgresIntegrationTest {

    private static final String TIMEZONE = "America/Guayaquil";
    private static final String INSTITUTION_NAME = "La Providencia";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndFindInstitutionMembership() {
        User user = createUser("teacher@school.edu.ec");
        Institution institution = createInstitution();

        InstitutionMembership saved = createMembership(user, institution, InstitutionRole.TEACHER);

        entityManager.flush();
        entityManager.clear();

        InstitutionMembership found = membershipRepository
                .findByUserIdAndInstitutionId(user.getId(), institution.getId())
                .orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(7, saved.getId().version());

        assertEquals(user.getId(), found.getUserId());
        assertEquals(institution.getId(), found.getInstitutionId());
        assertEquals(InstitutionRole.TEACHER, found.getInstitutionRole());
        assertTrue(found.isActive());

        List<InstitutionMembership> membershipsByUser = membershipRepository.findByUserId(user.getId());

        assertEquals(1, membershipsByUser.size());

        List<InstitutionMembership> membershipsByInstitution =
                membershipRepository.findByInstitutionId(institution.getId());

        assertEquals(1, membershipsByInstitution.size());
    }

    @Test
    void shouldRejectDuplicateMembershipForSameUserAndInstitution() {
        User user = createUser("teacher2@school.edu.ec");
        Institution institution = createInstitution();

        createMembership(user, institution, InstitutionRole.TEACHER);

        entityManager.flush();

        InstitutionMembership duplicateMembership =
                new InstitutionMembership(user.getId(), institution.getId(), InstitutionRole.ADMIN);

        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipRepository.save(duplicateMembership);
            entityManager.flush();
        });
    }

    private User createUser(String email) {
        return userRepository.save(new User(email));
    }

    private Institution createInstitution() {
        return institutionRepository.save(new Institution(INSTITUTION_NAME, TIMEZONE));
    }

    private InstitutionMembership createMembership(User user, Institution institution, InstitutionRole role) {

        return membershipRepository.save(new InstitutionMembership(user.getId(), institution.getId(), role));
    }
}
