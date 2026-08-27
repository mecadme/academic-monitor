package io.academicmonitor.identity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldNormalizeEmail() {
        User user = new User("  Profesor@Colegio.edu.ec  ");

        assertEquals("profesor@colegio.edu.ec", user.getEmail());
    }

    @Test
    void shouldStartAsRegularUser() {
        User user = new User("profesor@colegio.edu.ec");

        assertEquals(SystemRole.USER, user.getSystemRole());
        assertTrue(user.isActive());
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> new User("   "));
    }

    @Test
    void shouldPromoteToSuperAdmin() {
        User user = new User("admin@academicmonitor.io");

        user.promoteToSuperAdmin();

        assertEquals(SystemRole.SUPER_ADMIN, user.getSystemRole());
    }

    @Test
    void shouldRevokeSuperAdmin() {
        User user = new User("admin@academicmonitor.io");

        user.promoteToSuperAdmin();
        user.revokeSuperAdmin();

        assertEquals(SystemRole.USER, user.getSystemRole());
    }

    @Test
    void shouldDeactivateAndActivateUser() {
        User user = new User("profesor@colegio.edu.ec");

        user.deactivate();

        assertFalse(user.isActive());

        user.activate();

        assertTrue(user.isActive());
    }
}
