/******************************************************************************
 * DatabaseManager.java
 *
 * Author: James Human
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Central database connection manager using HikariCP connection pooling.
 * Loads connection settings from a properties file and provides pooled
 * connections to all DAO classes.
 ******************************************************************************/
package sentencebuilder.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    /**************************************************************************
     * Create a DatabaseManager by loading connection settings from a
     * properties file on the classpath (e.g., "db.properties").
     *
     * Expected properties: db.url, db.user, db.password, db.pool.size
     *
     * @param propertiesFile classpath resource name for the config file
     * @throws DatabaseException if the properties file cannot be loaded
     **************************************************************************/
    public DatabaseManager(String propertiesFile) {
        Properties props = new Properties();
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(propertiesFile)) {
            if (inputStream == null) {
                throw new DatabaseException(
                    "Properties file not found on classpath: " + propertiesFile);
            }
            props.load(inputStream);
        } catch (IOException ioException) {
            throw new DatabaseException(
                "Failed to load properties file: " + propertiesFile, ioException);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        config.setMaximumPoolSize(
            Integer.parseInt(props.getProperty("db.pool.size", "10")));

        // MySQL-specific optimizations for batch inserts
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    /**************************************************************************
     * Get a connection from the pool. Caller MUST close the connection
     * (use try-with-resources) to return it to the pool.
     *
     * @return a pooled database connection
     * @throws SQLException if a connection cannot be obtained
     **************************************************************************/
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**************************************************************************
     * Shut down the connection pool. Call this when the application exits.
     **************************************************************************/
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
