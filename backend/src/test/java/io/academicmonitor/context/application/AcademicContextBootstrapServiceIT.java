package io.academicmonitor.context.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.academicmonitor.context.config.AcademicContextProperties;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.institution.domain.Institution;
import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class AcademicContextBootstrapServiceIT extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repeatedRequestsPersistOnlyOneLocalContext() {
        AcademicContextBootstrapService service = new AcademicContextBootstrapService(
                userRepository,
                institutionRepository,
                membershipRepository,
                new AcademicContextProperties(
                        "local.teacher@academicmonitor.local", "Academic Monitor Local", "America/Guayaquil", ""),
                passwordEncoder);
        service.bootstrap();
        service.bootstrap();

        entityManager.flush();
        entityManager.clear();

        Long userCount = entityManager
                .createQuery("select count(u) from User u where u.email = :email", Long.class)
                .setParameter("email", "local.teacher@academicmonitor.local")
                .getSingleResult();

        Long institutionCount = entityManager
                .createQuery(
                        "select count(i) from Institution i where i.name = :name and i.timezone = :timezone",
                        Long.class)
                .setParameter("name", "Academic Monitor Local")
                .setParameter("timezone", "America/Guayaquil")
                .getSingleResult();

        User bootstrap = userRepository
                .findByEmail("local.teacher@academicmonitor.local")
                .orElseThrow();
        List<InstitutionMembership> memberships = membershipRepository.findByUserId(bootstrap.getId());

        assertEquals(1L, userCount);
        assertEquals(1L, institutionCount);
        assertEquals(1, memberships.size());
        assertEquals(InstitutionRole.TEACHER, memberships.get(0).getInstitutionRole());
    }

    @Test
    void addsArgonPasswordToExistingUserWithoutReplacingIdentityOrMembership() {
        User user = userRepository.save(new User("existing.teacher@example.test"));
        Institution institution = institutionRepository.save(new Institution("Existing school", "America/Guayaquil"));
        InstitutionMembership membership = membershipRepository.save(
                new InstitutionMembership(user.getId(), institution.getId(), InstitutionRole.TEACHER));
        AcademicContextBootstrapService service = new AcademicContextBootstrapService(
                userRepository,
                institutionRepository,
                membershipRepository,
                new AcademicContextProperties(
                        user.getEmail(), "Unused name", "America/Guayaquil", "test-only-password"),
                passwordEncoder);

        service.bootstrap();
        String hash = user.getPasswordHash();
        service.bootstrap();
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertEquals(user.getId(), reloaded.getId());
        assertEquals(hash, reloaded.getPasswordHash());
        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(passwordEncoder.matches("test-only-password", hash));
        List<InstitutionMembership> actualMemberships = membershipRepository.findByUserId(user.getId());
        assertEquals(1, actualMemberships.size());
        assertEquals(membership.getId(), actualMemberships.getFirst().getId());
    }
}
