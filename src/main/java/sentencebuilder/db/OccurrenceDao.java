/******************************************************************************
 * OccurrenceDao.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (DAO implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Data access object for the 'word_file_occurrences' table. Tracks how
 * many times each word appears in each imported file. Uses batch inserts
 * since a single file import may produce thousands of occurrences.
 ******************************************************************************/
package sentencebuilder.db;

import sentencebuilder.db.model.Occurrence;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OccurrenceDao {

    private final DatabaseManager dbManager;

    public OccurrenceDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**************************************************************************
     * Batch insert word-file occurrences for a single imported file.
     * The map keys are word_ids, values are occurrence counts.
     *
     * @param fileId     the file_id from imported_files
     * @param wordCounts map of word_id to occurrence count
     **************************************************************************/
    public void insertBatch(int fileId, Map<Integer, Integer> wordCounts) {
        String sql = "INSERT INTO word_file_occurrences (word_id, file_id, count) "
                   + "VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE count = VALUES(count)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> entry : wordCounts.entrySet()) {
                stmt.setInt(1, entry.getKey());
                stmt.setInt(2, fileId);
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to batch insert occurrences for file_id: " + fileId, sqlException);
        }
    }

    /**************************************************************************
     * Find all occurrences for a given file.
     *
     * @param fileId the file_id to look up
     * @return list of Occurrences
     **************************************************************************/
    public List<Occurrence> findByFileId(int fileId) {
        String sql = "SELECT * FROM word_file_occurrences WHERE file_id = ?";
        List<Occurrence> occurrences = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    Occurrence occ = new Occurrence();
                    occ.setWordId(resultSet.getInt("word_id"));
                    occ.setFileId(resultSet.getInt("file_id"));
                    occ.setCount(resultSet.getInt("count"));
                    occurrences.add(occ);
                }
            }
            return occurrences;
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to find occurrences for file_id: " + fileId, sqlException);
        }
    }
}
