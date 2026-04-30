/******************************************************************************
 * SentenceGenerator.java
 *
 * Author: James Human
 * Revised by: Murtaza Khan (refactoring and feature additions)
 * Revised Date: 4/29/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Generates sentences using word transition data from the database.
 * Supports three algorithms: WEIGHTED_RANDOM, MOST_FREQUENT, and MOST_FREQUENT_RANDOM.
 ******************************************************************************/
package sentencebuilder.algorithms;

import sentencebuilder.db.TransitionDao;
import sentencebuilder.db.WordDao;
import sentencebuilder.db.model.Transition;
import sentencebuilder.db.model.Word;

import java.util.List;
import java.util.Random;

public class SentenceGenerator {

    private final WordDao wordDao;
    private final TransitionDao transitionDao;
    private final Random random = new Random();

    public SentenceGenerator(WordDao wordDao, TransitionDao transitionDao) {
        this.wordDao = wordDao;
        this.transitionDao = transitionDao;
    }

    /**************************************************************************
     * Generate a sentence starting from the given seed word.
     *
     * @param seedWord  the word to start the sentence with
     * @param maxLength maximum number of words in the sentence
     * @param algorithm "WEIGHTED_RANDOM", "MOST_FREQUENT", or "MOST_FREQUENT_RANDOM"
     * @return the generated sentence, or an error message
     **************************************************************************/
    public String generate(String seedWord, int maxLength, String algorithm) {
        Word word = wordDao.findByWord(seedWord.toLowerCase().trim());
        if (word == null) {
            return "[Word '" + seedWord + "' not found in database]";
        }

        StringBuilder sentence = new StringBuilder(word.getWord());
        int currentWordId = word.getWordId();

        for (int step = 1; step < maxLength; step++) {
            List<Transition> nextTransitions;
            if ("MOST_FREQUENT".equals(algorithm) || "MOST_FREQUENT_RANDOM".equals(algorithm)) {
                nextTransitions = transitionDao.findByWordIdWeighted(currentWordId);
            } else {
                nextTransitions = transitionDao.findByWordId(currentWordId);
            }

            Transition chosen = null;
            Word nextWord = null;

            if (!nextTransitions.isEmpty()) {
                if ("MOST_FREQUENT".equals(algorithm)) {
                    // Always pick the most frequent next word
                    chosen = nextTransitions.get(0);
                } else if ("MOST_FREQUENT_RANDOM".equals(algorithm)) {
                    // Randomly pick from the top 5 most frequent transitions
                    int topN = Math.min(5, nextTransitions.size());
                    chosen = nextTransitions.get(random.nextInt(topN));
                } else {
                    // Weighted random selection
                    int totalWeight = 0;
                    for (Transition transition : nextTransitions) {
                        totalWeight += transition.getCount();
                    }
                    int randomValue = random.nextInt(totalWeight);
                    int cumulative = 0;
                    chosen = nextTransitions.get(0);
                    for (Transition transition : nextTransitions) {
                        cumulative += transition.getCount();
                        if (randomValue < cumulative) {
                            chosen = transition;
                            break;
                        }
                    }
                }
                nextWord = wordDao.findById(chosen.getNextWordId());
            } else {
                // No transitions found (e.g., user-added word with no corpus data)
                // Pick a random word from top frequent words to continue the sentence
                List<Word> topWords = wordDao.getTopN(20);
                if (!topWords.isEmpty()) {
                    nextWord = topWords.get(random.nextInt(topWords.size()));
                }
            }

            if (nextWord == null) break;

            sentence.append(" ").append(nextWord.getWord());
            currentWordId = nextWord.getWordId();
        }

        // Capitalize first letter and add period
        String result = sentence.toString();
        return result.substring(0, 1).toUpperCase() + result.substring(1) + ".";
    }
}
