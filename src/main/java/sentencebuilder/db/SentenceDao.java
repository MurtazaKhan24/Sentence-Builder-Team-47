/******************************************************************************
 * SentenceDao.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (DAO implementation), Murtaza Khan (refactoring and feature additions)
 * Revised Dates: 3/30/2026, 4/29/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Data access object for the 'generated_sentences' table. Stores sentences
 * produced by the generation algorithms and provides query methods for the
 * generation history view in the reports tab.
 ******************************************************************************/
package sentencebuilder.db;

import sentencebuilder.db.model.GeneratedSentence;
import sentencebuilder.db.model.SentenceDuplicate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SentenceDao {

    private final DatabaseManager dbManager;

    public SentenceDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**************************************************************************
     * Store a generated sentence and return its generated sentence_id.
     *
     * @param sentence the GeneratedSentence to store
     * @return the generated sentence_id
     **************************************************************************/
    public int insert(GeneratedSentence sentence) {
        String sql = "INSERT INTO generated_sentences (sentence_text, seed_word_id, "
                   + "algorithm, word_count) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, sentence.getSentenceText());
            if (sentence.getSeedWordId() != null) {
                stmt.setInt(2, sentence.getSeedWordId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, sentence.getAlgorithm());
            stmt.setInt(4, sentence.getWordCount());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            throw new DatabaseException("Insert succeeded but no sentence_id was generated");
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to insert generated sentence", sqlException);
        }
    }

    /**************************************************************************
     * Retrieve all generated sentences, newest first.
     * Used by the Generation History tab in the reports view.
     *
     * @return list of all GeneratedSentences
     **************************************************************************/
    public List<GeneratedSentence> findAll() {
        String sql = "SELECT * FROM generated_sentences ORDER BY generated_at DESC";
        List<GeneratedSentence> sentences = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                sentences.add(mapRow(resultSet));
            }
            return sentences;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to retrieve generated sentences", sqlException);
        }
    }

    /**************************************************************************
     * Find generated sentences filtered by algorithm type.
     *
     * @param algorithm the algorithm name (e.g., "WEIGHTED_RANDOM")
     * @return list of matching GeneratedSentences
     **************************************************************************/
    public List<GeneratedSentence> findByAlgorithm(String algorithm) {
        String sql = "SELECT * FROM generated_sentences WHERE algorithm = ? "
                   + "ORDER BY generated_at DESC";
        List<GeneratedSentence> sentences = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, algorithm);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    sentences.add(mapRow(resultSet));
                }
            }
            return sentences;
        } catch (SQLException sqlException) {
            throw new DatabaseException(
                "Failed to find sentences by algorithm: " + algorithm, sqlException);
        }
    }

    /**************************************************************************
     * Retrieve generated sentences that appear more than once, highest count
     * first. Used by the duplicate-sentence report view.
     *
     * @return list of duplicate sentence summaries
     **************************************************************************/
    public List<SentenceDuplicate> findDuplicates() {
        String sql = "SELECT sentence_text, COUNT(*) AS duplicate_count "
                   + "FROM generated_sentences "
                   + "GROUP BY sentence_text "
                   + "HAVING COUNT(*) > 1 "
                   + "ORDER BY duplicate_count DESC, sentence_text ASC";
        List<SentenceDuplicate> duplicates = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                SentenceDuplicate duplicate = new SentenceDuplicate();
                duplicate.setSentenceText(resultSet.getString("sentence_text"));
                duplicate.setDuplicateCount(resultSet.getInt("duplicate_count"));
                duplicates.add(duplicate);
            }
            return duplicates;
        } catch (SQLException sqlException) {
            throw new DatabaseException("Failed to retrieve duplicate sentences", sqlException);
        }
    }

    private GeneratedSentence mapRow(ResultSet resultSet) throws SQLException {
        GeneratedSentence sentence = new GeneratedSentence();
        sentence.setSentenceId(resultSet.getInt("sentence_id"));
        sentence.setSentenceText(resultSet.getString("sentence_text"));
        int seedWordId = resultSet.getInt("seed_word_id");
        sentence.setSeedWordId(resultSet.wasNull() ? null : seedWordId);
        sentence.setAlgorithm(resultSet.getString("algorithm"));
        sentence.setWordCount(resultSet.getInt("word_count"));
        sentence.setGeneratedAt(resultSet.getTimestamp("generated_at").toLocalDateTime());
        return sentence;
    }
}
