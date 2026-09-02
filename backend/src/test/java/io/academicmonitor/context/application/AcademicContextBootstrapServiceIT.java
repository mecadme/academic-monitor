package io.academicmonitor.context.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.academicmonitor.institution.domain.InstitutionMembership;
import io.academicmonitor.institution.domain.InstitutionMembershipRepository;
import io.academicmonitor.institution.domain.InstitutionRole;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AcademicContextBootstrapServiceIT extends PostgresIntegrationTest {

    @Autowired
    private AcademicContextBootstrapService service;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repeatedRequestsPersistOnlyOneLocalContext() {
        AcademicContextResult first = service.bootstrap();
        AcademicContextResult second = service.bootstrap();

        entityManager.flush();
        entityManager.clear();

        assertEquals(first, second);

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

        List<InstitutionMembership> memberships = membershipRepository.findByUserId(first.teacherUserId());

        assertEquals(1L, userCount);
        assertEquals(1L, institutionCount);
        assertEquals(1, memberships.size());
        assertEquals(InstitutionRole.TEACHER, memberships.get(0).getInstitutionRole());
    }
}
