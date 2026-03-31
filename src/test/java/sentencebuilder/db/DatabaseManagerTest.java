package sentencebuilder.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/******************************************************************************
 * DatabaseManagerTest.java
 *
 * Tests for DatabaseManager. Requires MySQL running locally with the
 * sentence_builder database created (run sentence_builder_schema.sql first).
 ******************************************************************************/
class DatabaseManagerTest {

    private DatabaseManager dbManager;

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testGetConnection() throws SQLException {
        dbManager = new DatabaseManager("db.properties");
        try (Connection conn = dbManager.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
        }
    }

    @Test
    void testMissingPropertiesFile() {
        assertThrows(DatabaseException.class,
            () -> new DatabaseManager("nonexistent.properties"),
            "Should throw DatabaseException for missing properties file");
    }

    @Test
    void testClose() throws SQLException {
        dbManager = new DatabaseManager("db.properties");
        try (Connection conn = dbManager.getConnection()) {
            assertNotNull(conn);
        }
        dbManager.close();
        assertThrows(SQLException.class, () -> dbManager.getConnection());
        dbManager = null;
    }
}
