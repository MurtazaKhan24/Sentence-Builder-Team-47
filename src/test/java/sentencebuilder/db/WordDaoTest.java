package sentencebuilder.db;

import org.junit.jupiter.api.*;
import sentencebuilder.db.model.Word;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/******************************************************************************
 * WordDaoTest.java
 *
 * Tests for WordDao. Requires MySQL running locally with the
 * sentence_builder database created.
 ******************************************************************************/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WordDaoTest {

    private static DatabaseManager dbManager;
    private static WordDao wordDao;

    @BeforeAll
    static void setUp() {
        dbManager = new DatabaseManager("db.properties");
        wordDao = new WordDao(dbManager);
    }

    @AfterAll
    static void tearDown() {
        // Clean up test data
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM word_file_occurrences");
            stmt.executeUpdate("DELETE FROM word_transitions");
            stmt.executeUpdate("DELETE FROM generated_sentences");
            stmt.executeUpdate("DELETE FROM words");
        } catch (Exception ignored) {}
        dbManager.close();
    }

    @Test
    @Order(1)
    void testInsertAndFindByWord() {
        Word word = new Word("hello", 5, 2, 1);
        int wordId = wordDao.insert(word);
        assertTrue(wordId > 0, "Insert should return a positive word_id");

        Word found = wordDao.findByWord("hello");
        assertNotNull(found, "Should find the inserted word");
        assertEquals("hello", found.getWord());
        assertEquals(5, found.getTotalCount());
        assertEquals(2, found.getStartCount());
        assertEquals(1, found.getEndCount());
    }

    @Test
    @Order(2)
    void testInsertDuplicateAddsCounts() {
        // Insert "hello" again — counts should accumulate
        Word word = new Word("hello", 3, 1, 0);
        wordDao.insert(word);

        Word found = wordDao.findByWord("hello");
        assertEquals(8, found.getTotalCount(), "total_count should be 5 + 3");
        assertEquals(3, found.getStartCount(), "start_count should be 2 + 1");
        assertEquals(1, found.getEndCount(), "end_count should be 1 + 0");
    }

    @Test
    @Order(3)
    void testInsertBatch() {
        List<Word> batch = Arrays.asList(
            new Word("apple", 10, 0, 0),
            new Word("banana", 7, 1, 2),
            new Word("cherry", 3, 0, 1)
        );
        wordDao.insertBatch(batch);

        assertNotNull(wordDao.findByWord("apple"));
        assertNotNull(wordDao.findByWord("banana"));
        assertNotNull(wordDao.findByWord("cherry"));
    }

    @Test
    @Order(4)
    void testFindById() {
        Word apple = wordDao.findByWord("apple");
        assertNotNull(apple);

        Word found = wordDao.findById(apple.getWordId());
        assertNotNull(found);
        assertEquals("apple", found.getWord());
    }

    @Test
    @Order(5)
    void testFindAll() {
        List<Word> allWords = wordDao.findAll();
        assertTrue(allWords.size() >= 4,
            "Should have at least hello, apple, banana, cherry");
    }

    @Test
    @Order(6)
    void testUpdate() {
        Word apple = wordDao.findByWord("apple");
        apple.setTotalCount(100);
        apple.setStartCount(10);
        apple.setEndCount(5);
        wordDao.update(apple);

        Word updated = wordDao.findByWord("apple");
        assertEquals(100, updated.getTotalCount());
        assertEquals(10, updated.getStartCount());
        assertEquals(5, updated.getEndCount());
    }

    @Test
    @Order(7)
    void testFindByPrefix() {
        List<Word> results = wordDao.findByPrefix("app", 10);
        assertFalse(results.isEmpty(), "Should find 'apple'");
        assertEquals("apple", results.get(0).getWord());
    }

    @Test
    @Order(8)
    void testGetTopN() {
        List<Word> topTwo = wordDao.getTopN(2);
        assertEquals(2, topTwo.size());
        // apple (100) should be first, hello (8) second
        assertTrue(topTwo.get(0).getTotalCount() >= topTwo.get(1).getTotalCount(),
            "Results should be ordered by total_count descending");
    }

    @Test
    @Order(9)
    void testFindByWordNotFound() {
        assertNull(wordDao.findByWord("nonexistent"));
    }

    @Test
    @Order(10)
    void testFindByIdNotFound() {
        assertNull(wordDao.findById(999999));
    }
}
