package io.academicmonitor.shared.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class AuthenticationMigrationIT {
    @Test
    void upgradingV8PreservesExistingTeacherMembershipAndCourseOwnership() throws SQLException {
        try (var postgres = new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("authentication_upgrade_test")
                .withUsername("test")
                .withPassword("test")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .target(MigrationVersion.fromVersion("8"))
                    .load()
                    .migrate();
            UUID user = UUID.randomUUID();
            UUID institution = UUID.randomUUID();
            UUID membership = UUID.randomUUID();
            UUID course = UUID.randomUUID();
            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                insert(connection, "INSERT INTO users(id,email) VALUES (?, 'existing.teacher@example.test')", user);
                insert(connection, "INSERT INTO institutions(id,name) VALUES (?, 'Existing school')", institution);
                insert(
                        connection,
                        "INSERT INTO institution_memberships(id,user_id,institution_id,institution_role) VALUES (?,?,?,'TEACHER')",
                        membership,
                        user,
                        institution);
                insert(
                        connection,
                        "INSERT INTO academic_courses(id,teacher_user_id,institution_id,platform_code,external_id,name) VALUES (?,?,?,'TEST','existing-course','Existing course')",
                        course,
                        user,
                        institution);
            }
            var flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .load();
            assertEquals(1, flyway.migrate().migrationsExecuted);
            assertEquals("9", flyway.info().current().getVersion().getVersion());
            flyway.validate();
            try (Connection connection = DriverManager.getConnection(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                    var statement = connection.prepareStatement(
                            "SELECT u.id,u.password_hash,m.id,m.institution_id,c.id,c.teacher_user_id,c.institution_id FROM users u JOIN institution_memberships m ON m.user_id=u.id JOIN academic_courses c ON c.teacher_user_id=u.id WHERE u.id=?")) {
                statement.setObject(1, user);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(user, result.getObject(1, UUID.class));
                    assertNull(result.getString(2));
                    assertEquals(membership, result.getObject(3, UUID.class));
                    assertEquals(institution, result.getObject(4, UUID.class));
                    assertEquals(course, result.getObject(5, UUID.class));
                    assertEquals(user, result.getObject(6, UUID.class));
                    assertEquals(institution, result.getObject(7, UUID.class));
                }
            }
        }
    }

    private void insert(Connection connection, String sql, UUID... ids) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.length; index++) {
                statement.setObject(index + 1, ids[index]);
            }
            statement.executeUpdate();
        }
    }
}
