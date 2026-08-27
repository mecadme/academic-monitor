package io.academicmonitor.identity.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import io.academicmonitor.identity.domain.SystemRole;
import io.academicmonitor.identity.domain.User;
import io.academicmonitor.identity.domain.UserRepository;
import io.academicmonitor.shared.integration.PostgresIntegrationTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryAdapterIT extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndFindUserByEmail() {
        User user = new User("Professor@School.edu.ec");

        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("professor@school.edu.ec");

        assertTrue(found.isPresent());
        assertNotNull(saved.getId());

        User persisted = found.orElseThrow();

        assertEquals("professor@school.edu.ec", persisted.getEmail());

        assertEquals(SystemRole.USER, persisted.getSystemRole());

        assertTrue(persisted.isActive());
    }
}
