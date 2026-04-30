/******************************************************************************
 * SpellChecker.java
 * 
 * Author: Murtaza Khan
 * Date implemented: 4/29/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Utility class for spell checking using Levenshtein distance.
 * Finds similar words from the corpus based on edit distance.
 ******************************************************************************/
package sentencebuilder.utils;

import sentencebuilder.db.WordDao;
import sentencebuilder.db.model.Word;

import java.util.*;

public class SpellChecker {
    
    private final WordDao wordDao;
    private static final int MAX_DISTANCE = 2; // Words within 2 edits are suggestions
    private static final int SUGGESTION_LIMIT = 5;

    public SpellChecker(WordDao wordDao) {
        this.wordDao = wordDao;
    }

    /**************************************************************************
     * Calculate Levenshtein distance (edit distance) between two strings.
     * Returns the minimum number of single-character edits needed to transform
     * one string into another.
     *
     * @param s1 first string
     * @param s2 second string
     * @return the edit distance
     **************************************************************************/
    public static int levenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }

    /**************************************************************************
     * Check if a word is likely misspelled (not found in corpus).
     *
     * @param word the word to check
     * @return true if word not found in database
     **************************************************************************/
    public boolean isMisspelled(String word) {
        Word dbWord = wordDao.findByWord(word.toLowerCase().trim());
        return dbWord == null;
    }

    /**************************************************************************
     * Get spelling suggestions for a potentially misspelled word.
     * Returns words from the corpus that are similar (within MAX_DISTANCE edits).
     *
     * @param word the misspelled word
     * @return list of suggestions sorted by distance (closest first)
     **************************************************************************/
    public List<String> getSuggestions(String word) {
        List<Word> allWords = wordDao.findAll();
        Map<String, Integer> distances = new HashMap<>();

        for (Word dbWord : allWords) {
            int distance = levenshteinDistance(word, dbWord.getWord());
            if (distance > 0 && distance <= MAX_DISTANCE) {
                distances.put(dbWord.getWord(), distance);
            }
        }

        // Sort by distance (ascending) then by frequency (descending)
        List<String> suggestions = new ArrayList<>(distances.keySet());
        suggestions.sort((w1, w2) -> {
            int distDiff = distances.get(w1) - distances.get(w2);
            if (distDiff != 0) return distDiff;
            // Same distance - prefer more frequent words
            Word word1 = wordDao.findByWord(w1);
            Word word2 = wordDao.findByWord(w2);
            return word2.getTotalCount() - word1.getTotalCount();
        });

        return suggestions.subList(0, Math.min(SUGGESTION_LIMIT, suggestions.size()));
    }
}
