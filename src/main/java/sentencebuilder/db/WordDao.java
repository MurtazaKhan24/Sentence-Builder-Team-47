/******************************************************************************
 * WordDao.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (DAO implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Data access object for the 'words' table. Provides methods to insert,
 * query, update, and batch-load word records. Supports auto-complete
 * prefix search and top-N frequency queries for the reports view.
 ******************************************************************************/
package sentencebuilder.db;

import sentencebuilder.db.model.Word;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WordDao {

    private final DatabaseManager dbManager;

    public WordDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**************************************************************************
     * Insert a single word. Returns the generated word_id.
     * If the word already exists, updates its counts instead.
     *
     * @param word the Word to insert
     * @return the generated or existing word_id
     **************************************************************************/
    public int insert(Word word) {
        String sql = "INSERT INTO words (word, total_count, start_count, end_count, user_added) "
                   + "VALUES (?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "total_count = total_count + VALUES(total_count), "
                   + "start_count = start_count + VALUES(start_count), "
                   + "end_count = end_count + VALUES(end_count)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, word.getWord());
            stmt.setInt(2, word.getTotalCount());
            stmt.setInt(3, word.getStartCount());
            stmt.setInt(4, word.getEndCount());
            stmt.setBoolean(5, word.isUserAdded());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            // If ON DUPLICATE KEY UPDATE fired, no generated key — look up by word
            Word existing = findByWord(word.getWord());
            return existing != null ? existing.getWordId() : -1;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to insert word: " + word.getWord(), sqlException);
        }
    }

    /**************************************************************************
     * Batch insert a list of words. Uses ON DUPLICATE KEY UPDATE to handle
     * words that already exist by adding to their counts.
     *
     * @param words the list of Words to insert
     **************************************************************************/
    public void insertBatch(List<Word> words) {
        String sql = "INSERT INTO words (word, total_count, start_count, end_count, user_added) "
                   + "VALUES (?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "total_count = total_count + VALUES(total_count), "
                   + "start_count = start_count + VALUES(start_count), "
                   + "end_count = end_count + VALUES(end_count)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Word word : words) {
                stmt.setString(1, word.getWord());
                stmt.setInt(2, word.getTotalCount());
                stmt.setInt(3, word.getStartCount());
                stmt.setInt(4, word.getEndCount());
                stmt.setBoolean(5, word.isUserAdded());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to batch insert words", sqlException);
        }
    }

    /**************************************************************************
     * Find a word by its primary key.
     *
     * @param wordId the word_id to look up
     * @return the Word, or null if not found
     **************************************************************************/
    public Word findById(int wordId) {
        String sql = "SELECT * FROM words WHERE word_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
            return null;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to find word by id: " + wordId, sqlException);
        }
    }

    /**************************************************************************
     * Find a word by its text value.
     *
     * @param word the word text to look up
     * @return the Word, or null if not found
     **************************************************************************/
    public Word findByWord(String word) {
        String sql = "SELECT * FROM words WHERE word = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, word);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
            return null;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to find word: " + word, sqlException);
        }
    }

    /**************************************************************************
     * Retrieve all words from the database. Used by the admin word
     * management view.
     *
     * @return list of all Words
     **************************************************************************/
    public List<Word> findAll() {
        String sql = "SELECT * FROM words ORDER BY word";
        List<Word> words = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                words.add(mapRow(resultSet));
            }
            return words;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to retrieve all words", sqlException);
        }
    }

    /**************************************************************************
     * Update word metadata (counts and user_added flag). Used by the
     * admin word management view's "Save Changes" button.
     *
     * @param word the Word with updated fields
     **************************************************************************/
    public void update(Word word) {
        String sql = "UPDATE words SET total_count = ?, start_count = ?, "
                   + "end_count = ?, user_added = ? WHERE word_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, word.getTotalCount());
            stmt.setInt(2, word.getStartCount());
            stmt.setInt(3, word.getEndCount());
            stmt.setBoolean(4, word.isUserAdded());
            stmt.setInt(5, word.getWordId());
            stmt.executeUpdate();
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to update word: " + word.getWord(), sqlException);
        }
    }

    /**************************************************************************
     * Find words that start with the given prefix, ordered by frequency.
     * Used by the auto-complete feature.
     *
     * @param prefix the prefix to search for
     * @param limit  maximum number of results to return
     * @return list of matching Words, highest frequency first
     **************************************************************************/
    public List<Word> findByPrefix(String prefix, int limit) {
        String sql = "SELECT * FROM words WHERE word LIKE ? ORDER BY total_count DESC LIMIT ?";
        List<Word> words = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            stmt.setInt(2, limit);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    words.add(mapRow(resultSet));
                }
            }
            return words;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to find words by prefix: " + prefix, sqlException);
        }
    }

    /**************************************************************************
     * Get the top N most frequent words. Used by the reports view.
     *
     * @param topN number of words to return
     * @return list of Words ordered by total_count descending
     **************************************************************************/
    public List<Word> getTopN(int topN) {
        String sql = "SELECT * FROM words ORDER BY total_count DESC LIMIT ?";
        List<Word> words = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, topN);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    words.add(mapRow(resultSet));
                }
            }
            return words;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to get top " + topN + " words", sqlException);
        }
    }

    /**************************************************************************
     * Map a ResultSet row to a Word object.
     **************************************************************************/
    private Word mapRow(ResultSet resultSet) throws SQLException {
        Word word = new Word();
        word.setWordId(resultSet.getInt("word_id"));
        word.setWord(resultSet.getString("word"));
        word.setTotalCount(resultSet.getInt("total_count"));
        word.setStartCount(resultSet.getInt("start_count"));
        word.setEndCount(resultSet.getInt("end_count"));
        word.setUserAdded(resultSet.getBoolean("user_added"));
        word.setAddedAt(resultSet.getTimestamp("added_at").toLocalDateTime());
        return word;
    }
}
