/******************************************************************************
 * FileDao.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (DAO implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Data access object for the 'imported_files' table. Tracks metadata about
 * each text file imported into the system, including word and sentence counts.
 ******************************************************************************/
package sentencebuilder.db;

import sentencebuilder.db.model.ImportedFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDao {

    private final DatabaseManager dbManager;

    public FileDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**************************************************************************
     * Insert a file record and return the generated file_id.
     *
     * @param file the ImportedFile to insert
     * @return the generated file_id
     **************************************************************************/
    public int insert(ImportedFile file) {
        String sql = "INSERT INTO imported_files (filename, filepath, word_count, "
                   + "sentence_count, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, file.getFilename());
            stmt.setString(2, file.getFilepath());
            stmt.setInt(3, file.getWordCount());
            stmt.setInt(4, file.getSentenceCount());
            stmt.setString(5, file.getNotes());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            throw new DatabaseException("Insert succeeded but no file_id was generated");
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to insert file: " + file.getFilename(), sqlException);
        }
    }

    /**************************************************************************
     * Retrieve all imported files.
     *
     * @return list of all ImportedFiles, ordered by import date descending
     **************************************************************************/
    public List<ImportedFile> findAll() {
        String sql = "SELECT * FROM imported_files ORDER BY imported_at DESC";
        List<ImportedFile> files = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                files.add(mapRow(resultSet));
            }
            return files;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to retrieve imported files", sqlException);
        }
    }

    /**************************************************************************
     * Find an imported file by its filepath. Used to check if a file has
     * already been imported (filepath is UNIQUE in the schema).
     *
     * @param filepath the file path to search for
     * @return the ImportedFile, or null if not found
     **************************************************************************/
    public ImportedFile findByFilepath(String filepath) {
        String sql = "SELECT * FROM imported_files WHERE filepath = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filepath);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
            return null;
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to find file by path: " + filepath, sqlException);
        }
    }

    private ImportedFile mapRow(ResultSet resultSet) throws SQLException {
        ImportedFile file = new ImportedFile();
        file.setFileId(resultSet.getInt("file_id"));
        file.setFilename(resultSet.getString("filename"));
        file.setFilepath(resultSet.getString("filepath"));
        file.setWordCount(resultSet.getInt("word_count"));
        file.setSentenceCount(resultSet.getInt("sentence_count"));
        file.setImportedAt(resultSet.getTimestamp("imported_at").toLocalDateTime());
        file.setNotes(resultSet.getString("notes"));
        return file;
    }
}
