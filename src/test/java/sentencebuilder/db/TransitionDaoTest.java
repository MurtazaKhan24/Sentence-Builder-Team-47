package sentencebuilder.db;

import org.junit.jupiter.api.*;
import sentencebuilder.db.model.Transition;
import sentencebuilder.db.model.Word;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransitionDaoTest {

    private static DatabaseManager dbManager;
    private static TransitionDao transitionDao;
    private static WordDao wordDao;
    private static int wordIdThe;
    private static int wordIdQuick;
    private static int wordIdFox;

    @BeforeAll
    static void setUp() {
        dbManager = new DatabaseManager("db.properties");
        transitionDao = new TransitionDao(dbManager);
        wordDao = new WordDao(dbManager);

        // Insert prerequisite words for transitions
        wordIdThe = wordDao.insert(new Word("the", 10, 5, 0));
        wordIdQuick = wordDao.insert(new Word("quick", 4, 0, 0));
        wordIdFox = wordDao.insert(new Word("fox", 3, 0, 2));
    }

    @AfterAll
    static void tearDown() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM word_transitions");
            stmt.executeUpdate("DELETE FROM word_file_occurrences");
            stmt.executeUpdate("DELETE FROM generated_sentences");
            stmt.executeUpdate("DELETE FROM words");
        } catch (Exception ignored) {}
        dbManager.close();
    }

    @Test
    @Order(1)
    void testInsert() {
        Transition transition = new Transition(wordIdThe, wordIdQuick, 3);
        transitionDao.insert(transition);

        List<Transition> results = transitionDao.findByWordId(wordIdThe);
        assertFalse(results.isEmpty(), "Should find the inserted transition");
        assertEquals(wordIdQuick, results.get(0).getNextWordId());
        assertEquals(3, results.get(0).getCount());
    }

    @Test
    @Order(2)
    void testInsertDuplicateAddsCounts() {
        // Insert the->quick again with count 2
        Transition transition = new Transition(wordIdThe, wordIdQuick, 2);
        transitionDao.insert(transition);

        List<Transition> results = transitionDao.findByWordId(wordIdThe);
        assertEquals(5, results.get(0).getCount(), "Count should be 3 + 2");
    }

    @Test
    @Order(3)
    void testInsertBatch() {
        List<Transition> batch = Arrays.asList(
            new Transition(wordIdThe, wordIdFox, 1),
            new Transition(wordIdQuick, wordIdFox, 7)
        );
        transitionDao.insertBatch(batch);

        List<Transition> fromThe = transitionDao.findByWordId(wordIdThe);
        assertEquals(2, fromThe.size(), "the -> quick and the -> fox");

        List<Transition> fromQuick = transitionDao.findByWordId(wordIdQuick);
        assertEquals(1, fromQuick.size(), "quick -> fox");
    }

    @Test
    @Order(4)
    void testFindByWordIdWeighted() {
        List<Transition> weighted = transitionDao.findByWordIdWeighted(wordIdThe);
        assertEquals(2, weighted.size());
        // the->quick (5) should come before the->fox (1)
        assertTrue(weighted.get(0).getCount() >= weighted.get(1).getCount(),
            "Should be ordered by count descending");
    }

    @Test
    @Order(5)
    void testFindByWordIdNoResults() {
        List<Transition> results = transitionDao.findByWordId(999999);
        assertTrue(results.isEmpty());
    }
}
