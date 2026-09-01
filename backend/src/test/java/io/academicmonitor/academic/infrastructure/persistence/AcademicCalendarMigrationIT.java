package io.academicmonitor.academic.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class AcademicCalendarMigrationIT {

    @Test
    void migratesLegacyCoursesAndActivitiesWithoutRequiringAcademicContext() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("academic_calendar_migration_test")
                .withUsername("test")
                .withPassword("test")) {
            postgres.start();

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .target(MigrationVersion.fromVersion("2"))
                    .load()
                    .migrate();

            UUID courseId = UUID.randomUUID();
            UUID activityId = UUID.randomUUID();
            insertLegacyData(postgres, courseId, activityId);

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                assertNull(findUuid(connection, "academic_courses", "academic_year_id", courseId));
                assertNull(findUuid(connection, "activities", "academic_period_id", activityId));
            }
        }
    }

    private static void insertLegacyData(PostgreSQLContainer<?> postgres, UUID courseId, UUID activityId)
            throws SQLException {
        UUID institutionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        try (Connection connection =
                DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            executeInsert(
                    connection,
                    "INSERT INTO institutions (id, name) VALUES (?, ?)",
                    institutionId,
                    "Legacy Test School");
            executeInsert(
                    connection,
                    "INSERT INTO users (id, email) VALUES (?, ?)",
                    teacherId,
                    "legacy.teacher@example.test");

            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO academic_courses "
                    + "(id, institution_id, teacher_user_id, platform_code, external_id, name) "
                    + "VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setObject(1, courseId);
                statement.setObject(2, institutionId);
                statement.setObject(3, teacherId);
                statement.setString(4, "TEST");
                statement.setString(5, "legacy-course");
                statement.setString(6, "Legacy Course");
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO activities (id, course_id, platform_code, external_id, name) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                statement.setObject(1, activityId);
                statement.setObject(2, courseId);
                statement.setString(3, "TEST");
                statement.setString(4, "legacy-activity");
                statement.setString(5, "Legacy Activity");
                statement.executeUpdate();
            }
        }
    }

    private static void executeInsert(Connection connection, String sql, UUID id, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    private static UUID findUuid(Connection connection, String table, String column, UUID id) throws SQLException {
        String sql = "SELECT " + column + " FROM " + table + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getObject(1, UUID.class);
            }
        }
    }
}
