/******************************************************************************
 * ImportService.java
 *
 * Author: Murtaza Khan
 * Revised by: James Human (converted from Python to Java)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Orchestrates the import pipeline: parse a text file, then store words,
 * transitions, and file metadata in the database via the DAO layer.
 * Based on gutenberg_script.py by Murtaza Khan.
 ******************************************************************************/
package sentencebuilder.parser;

import sentencebuilder.db.*;
import sentencebuilder.db.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ImportService {

    private final WordDao wordDao;
    private final TransitionDao transitionDao;
    private final FileDao fileDao;
    private final OccurrenceDao occurrenceDao;

    public ImportService(WordDao wordDao, TransitionDao transitionDao,
                         FileDao fileDao, OccurrenceDao occurrenceDao) {
        this.wordDao = wordDao;
        this.transitionDao = transitionDao;
        this.fileDao = fileDao;
        this.occurrenceDao = occurrenceDao;
    }

    /**************************************************************************
     * Import a text file: parse it, store words and transitions in the DB.
     *
     * @param file the text file to import
     * @return the number of unique words imported
     * @throws IOException if the file cannot be read
     **************************************************************************/
    public int importFile(File file) throws IOException {
        // Check if already imported
        ImportedFile existing = fileDao.findByFilepath(file.getAbsolutePath());
        if (existing != null) {
            return -1; // Already imported
        }

        // Parse the file
        TextParser parser = new TextParser();
        parser.parse(file);

        // Insert words in batch
        List<Word> wordList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : parser.getWordFrequencies().entrySet()) {
            String wordText = entry.getKey();
            int totalCount = entry.getValue();
            int startCount = parser.getStartCounts().getOrDefault(wordText, 0);
            int endCount = parser.getEndCounts().getOrDefault(wordText, 0);
            wordList.add(new Word(wordText, totalCount, startCount, endCount));
        }
        wordDao.insertBatch(wordList);

        // Build a map of word text -> word_id for transition insertion
        Map<String, Integer> wordIdMap = new HashMap<>();
        Map<Integer, Integer> wordCountsForFile = new HashMap<>();
        for (Map.Entry<String, Integer> entry : parser.getWordFrequencies().entrySet()) {
            Word dbWord = wordDao.findByWord(entry.getKey());
            if (dbWord != null) {
                wordIdMap.put(entry.getKey(), dbWord.getWordId());
                wordCountsForFile.put(dbWord.getWordId(), entry.getValue());
            }
        }

        // Insert transitions in batch
        List<Transition> transitionList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : parser.getTransitions().entrySet()) {
            Integer fromId = wordIdMap.get(entry.getKey());
            if (fromId == null) continue;
            for (Map.Entry<String, Integer> next : entry.getValue().entrySet()) {
                Integer toId = wordIdMap.get(next.getKey());
                if (toId == null) continue;
                transitionList.add(new Transition(fromId, toId, next.getValue()));
            }
        }
        transitionDao.insertBatch(transitionList);

        // Record the imported file
        ImportedFile importedFile = new ImportedFile(file.getName(), file.getAbsolutePath());
        importedFile.setWordCount(parser.getTotalWords());
        importedFile.setSentenceCount(parser.getTotalSentences());
        int fileId = fileDao.insert(importedFile);

        // Insert occurrences
        occurrenceDao.insertBatch(fileId, wordCountsForFile);

        return parser.getWordFrequencies().size();
    }
}
