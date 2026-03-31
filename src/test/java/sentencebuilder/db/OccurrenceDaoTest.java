package sentencebuilder.db;

import org.junit.jupiter.api.*;
import sentencebuilder.db.model.ImportedFile;
import sentencebuilder.db.model.Occurrence;
import sentencebuilder.db.model.Word;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OccurrenceDaoTest {

    private static DatabaseManager dbManager;
    private static OccurrenceDao occurrenceDao;
    private static int fileId;
    private static int wordId1;
    private static int wordId2;

    @BeforeAll
    static void setUp() {
        dbManager = new DatabaseManager("db.properties");
        occurrenceDao = new OccurrenceDao(dbManager);

        // Insert prerequisite data
        WordDao wordDao = new WordDao(dbManager);
        wordId1 = wordDao.insert(new Word("test1", 5, 0, 0));
        wordId2 = wordDao.insert(new Word("test2", 3, 0, 0));

        FileDao fileDao = new FileDao(dbManager);
        ImportedFile file = new ImportedFile("test.txt", "/test/occ_test.txt");
        file.setWordCount(8);
        file.setSentenceCount(2);
        fileId = fileDao.insert(file);
    }

    @AfterAll
    static void tearDown() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM word_file_occurrences");
            stmt.executeUpdate("DELETE FROM word_transitions");
            stmt.executeUpdate("DELETE FROM generated_sentences");
            stmt.executeUpdate("DELETE FROM imported_files");
            stmt.executeUpdate("DELETE FROM words");
        } catch (Exception ignored) {}
        dbManager.close();
    }

    @Test
    @Order(1)
    void testInsertBatchAndFindByFileId() {
        Map<Integer, Integer> wordCounts = new HashMap<>();
        wordCounts.put(wordId1, 5);
        wordCounts.put(wordId2, 3);

        occurrenceDao.insertBatch(fileId, wordCounts);

        List<Occurrence> results = occurrenceDao.findByFileId(fileId);
        assertEquals(2, results.size());
    }

    @Test
    @Order(2)
    void testFindByFileIdNoResults() {
        List<Occurrence> results = occurrenceDao.findByFileId(999999);
        assertTrue(results.isEmpty());
    }
}
