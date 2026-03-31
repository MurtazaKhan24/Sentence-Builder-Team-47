/******************************************************************************
 * FileDaoTest.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
 *
 * Tests for FileDao. Requires MySQL running locally with the
 * sentence_builder database created.
 ******************************************************************************/
package sentencebuilder.db;

import org.junit.jupiter.api.*;
import sentencebuilder.db.model.ImportedFile;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileDaoTest {

    private static DatabaseManager dbManager;
    private static FileDao fileDao;

    @BeforeAll
    static void setUp() {
        dbManager = new DatabaseManager("db.properties");
        fileDao = new FileDao(dbManager);
    }

    @AfterAll
    static void tearDown() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM word_file_occurrences");
            stmt.executeUpdate("DELETE FROM imported_files");
        } catch (Exception ignored) {}
        dbManager.close();
    }

    @Test
    @Order(1)
    void testInsertAndFindByFilepath() {
        ImportedFile file = new ImportedFile("pride.txt", "/data/pride.txt");
        file.setWordCount(120000);
        file.setSentenceCount(5400);
        file.setNotes("Pride and Prejudice");

        int fileId = fileDao.insert(file);
        assertTrue(fileId > 0);

        ImportedFile found = fileDao.findByFilepath("/data/pride.txt");
        assertNotNull(found);
        assertEquals("pride.txt", found.getFilename());
        assertEquals(120000, found.getWordCount());
        assertEquals("Pride and Prejudice", found.getNotes());
    }

    @Test
    @Order(2)
    void testFindAll() {
        // Insert a second file
        ImportedFile file2 = new ImportedFile("moby.txt", "/data/moby.txt");
        file2.setWordCount(210000);
        file2.setSentenceCount(9500);
        fileDao.insert(file2);

        List<ImportedFile> allFiles = fileDao.findAll();
        assertTrue(allFiles.size() >= 2);
    }

    @Test
    @Order(3)
    void testFindByFilepathNotFound() {
        assertNull(fileDao.findByFilepath("/nonexistent/path.txt"));
    }

    @Test
    @Order(4)
    void testInsertDuplicateFilepathFails() {
        ImportedFile duplicate = new ImportedFile("pride2.txt", "/data/pride.txt");
        assertThrows(DatabaseException.class, () -> fileDao.insert(duplicate),
            "Should fail on duplicate filepath (UNIQUE constraint)");
    }
}
