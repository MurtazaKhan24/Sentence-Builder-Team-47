/******************************************************************************
 * TransitionDao.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (DAO implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Data access object for the 'word_transitions' table. Provides methods to
 * insert and query word-to-word transitions. The sentence generation
 * algorithms use findByWordId() and findByWordIdWeighted() to walk
 * the transition graph.
 ******************************************************************************/
package sentencebuilder.db;

import sentencebuilder.db.model.Transition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransitionDao {

    private final DatabaseManager dbManager;

    public TransitionDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**************************************************************************
     * Insert a single transition. If the (word_id, next_word_id) pair
     * already exists, adds to the existing count.
     *
     * @param transition the Transition to insert
     **************************************************************************/
    public void insert(Transition transition) {
        String sql = "INSERT INTO word_transitions (word_id, next_word_id, count) "
                   + "VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE count = count + VALUES(count)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transition.getWordId());
            stmt.setInt(2, transition.getNextWordId());
            stmt.setInt(3, transition.getCount());
            stmt.executeUpdate();
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to insert transition", sqlException);
        }
    }

    /**************************************************************************
     * Batch insert transitions. Uses ON DUPLICATE KEY UPDATE to accumulate
     * counts for pairs that already exist.
     *
     * @param transitions the list of Transitions to insert
     **************************************************************************/
    public void insertBatch(List<Transition> transitions) {
        String sql = "INSERT INTO word_transitions (word_id, next_word_id, count) "
                   + "VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE count = count + VALUES(count)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Transition transition : transitions) {
                stmt.setInt(1, transition.getWordId());
                stmt.setInt(2, transition.getNextWordId());
                stmt.setInt(3, transition.getCount());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to batch insert transitions", sqlException);
        }
    }

    /**************************************************************************
     * Find all transitions from a given word. Returns transitions in
     * no particular order. Used by the random walk algorithm.
     *
     * @param wordId the source word_id
     * @return list of Transitions from this word
     **************************************************************************/
    public List<Transition> findByWordId(int wordId) {
        String sql = "SELECT * FROM word_transitions WHERE word_id = ?";
        List<Transition> transitions = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    transitions.add(mapRow(resultSet));
                }
            }
            return transitions;
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to find transitions for word_id: " + wordId, sqlException);
        }
    }

    /**************************************************************************
     * Find all transitions from a given word, ordered by count descending.
     * Used by the weighted probability and most-frequent algorithms so the
     * highest-count transition is first.
     *
     * @param wordId the source word_id
     * @return list of Transitions ordered by count descending
     **************************************************************************/
    public List<Transition> findByWordIdWeighted(int wordId) {
        String sql = "SELECT * FROM word_transitions WHERE word_id = ? ORDER BY count DESC";
        List<Transition> transitions = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    transitions.add(mapRow(resultSet));
                }
            }
            return transitions;
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to find weighted transitions for word_id: " + wordId, sqlException);
        }
    }

    /**************************************************************************
     * Map a ResultSet row to a Transition object.
     **************************************************************************/
    private Transition mapRow(ResultSet resultSet) throws SQLException {
        Transition transition = new Transition();
        transition.setTransitionId(resultSet.getInt("transition_id"));
        transition.setWordId(resultSet.getInt("word_id"));
        transition.setNextWordId(resultSet.getInt("next_word_id"));
        transition.setCount(resultSet.getInt("count"));
        return transition;
    }
}
