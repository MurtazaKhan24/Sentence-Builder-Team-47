/******************************************************************************
 * TextParser.java
 *
 * Author: Murtaza Khan
 * Revised by: James Human (converted from Python to Java)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Simple text parser that reads a .txt file, extracts words, counts
 * frequencies, identifies sentence boundaries, and builds word transitions.
 * Based on gutenberg_script.py by Murtaza Khan.
 ******************************************************************************/
package sentencebuilder.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TextParser {

    private final Map<String, Integer> wordFrequencies = new HashMap<>();
    private final Map<String, Integer> startCounts = new HashMap<>();
    private final Map<String, Integer> endCounts = new HashMap<>();
    private final Map<String, Map<String, Integer>> transitions = new HashMap<>();
    private int totalWords = 0;
    private int totalSentences = 0;

    /**************************************************************************
     * Parse a text file and extract word frequencies and transitions.
     *
     * @param file the text file to parse
     * @throws IOException if the file cannot be read
     **************************************************************************/
    public void parse(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder textBuffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                textBuffer.append(line).append(" ");
            }
            String fullText = textBuffer.toString();
            processText(fullText);
        }
    }

    private void processText(String text) {
        // Split into sentences on . ! ?
        String[] sentences = text.split("[.!?]+");

        for (String sentence : sentences) {
            String[] words = sentence.trim().toLowerCase()
                .replaceAll("[^a-z\\s'-]", "")
                .split("\\s+");

            // Filter out empty strings
            List<String> wordList = new ArrayList<>();
            for (String word : words) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty() && trimmed.length() > 1) {
                    wordList.add(trimmed);
                }
            }

            if (wordList.isEmpty()) continue;

            totalSentences++;

            // Count start and end words
            startCounts.merge(wordList.get(0), 1, Integer::sum);
            endCounts.merge(wordList.get(wordList.size() - 1), 1, Integer::sum);

            // Count word frequencies and transitions
            for (int ix = 0; ix < wordList.size(); ix++) {
                String currentWord = wordList.get(ix);
                wordFrequencies.merge(currentWord, 1, Integer::sum);
                totalWords++;

                // Build transition to next word
                if (ix < wordList.size() - 1) {
                    String nextWord = wordList.get(ix + 1);
                    transitions.computeIfAbsent(currentWord, k -> new HashMap<>())
                        .merge(nextWord, 1, Integer::sum);
                }
            }
        }
    }

    public Map<String, Integer> getWordFrequencies() { return wordFrequencies; }
    public Map<String, Integer> getStartCounts() { return startCounts; }
    public Map<String, Integer> getEndCounts() { return endCounts; }
    public Map<String, Map<String, Integer>> getTransitions() { return transitions; }
    public int getTotalWords() { return totalWords; }
    public int getTotalSentences() { return totalSentences; }
}
