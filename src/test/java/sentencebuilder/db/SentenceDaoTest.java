/******************************************************************************
 * SentenceDaoTest.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
 *
 * Tests for SentenceDao. Requires MySQL running locally with the
 * sentence_builder database created.
 ******************************************************************************/
package sentencebuilder.db;

import org.junit.jupiter.api.*;
import sentencebuilder.db.model.GeneratedSentence;
import sentencebuilder.db.model.Word;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SentenceDaoTest {

    private static DatabaseManager dbManager;
    private static SentenceDao sentenceDao;
    private static int seedWordId;

    @BeforeAll
    static void setUp() {
        dbManager = new DatabaseManager("db.properties");
        sentenceDao = new SentenceDao(dbManager);

        // Insert a seed word
        WordDao wordDao = new WordDao(dbManager);
        seedWordId = wordDao.insert(new Word("the", 100, 50, 0));
    }

    @AfterAll
    static void tearDown() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM generated_sentences");
            stmt.executeUpdate("DELETE FROM word_transitions");
            stmt.executeUpdate("DELETE FROM word_file_occurrences");
            stmt.executeUpdate("DELETE FROM words");
        } catch (Exception ignored) {}
        dbManager.close();
    }

    @Test
    @Order(1)
    void testInsertAndFindAll() {
        GeneratedSentence sentence = new GeneratedSentence(
            "the quick brown fox jumps", seedWordId, "WEIGHTED_RANDOM", 5);
        int sentenceId = sentenceDao.insert(sentence);
        assertTrue(sentenceId > 0);

        List<GeneratedSentence> all = sentenceDao.findAll();
        assertFalse(all.isEmpty());
        assertEquals("the quick brown fox jumps", all.get(0).getSentenceText());
        assertEquals("WEIGHTED_RANDOM", all.get(0).getAlgorithm());
    }

    @Test
    @Order(2)
    void testInsertWithNullSeedWord() {
        GeneratedSentence sentence = new GeneratedSentence(
            "a random sentence", null, "MOST_FREQUENT", 3);
        int sentenceId = sentenceDao.insert(sentence);
        assertTrue(sentenceId > 0);
    }

    @Test
    @Order(3)
    void testFindByAlgorithm() {
        List<GeneratedSentence> weighted = sentenceDao.findByAlgorithm("WEIGHTED_RANDOM");
        assertEquals(1, weighted.size());

        List<GeneratedSentence> frequent = sentenceDao.findByAlgorithm("MOST_FREQUENT");
        assertEquals(1, frequent.size());

        List<GeneratedSentence> none = sentenceDao.findByAlgorithm("NONEXISTENT");
        assertTrue(none.isEmpty());
    }
}
