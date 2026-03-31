/******************************************************************************
 * SentenceGenerator.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
 *
 * Generates sentences using word transition data from the database.
 * Supports two algorithms: WEIGHTED_RANDOM and MOST_FREQUENT.
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
     * @param algorithm "WEIGHTED_RANDOM" or "MOST_FREQUENT"
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
            if ("MOST_FREQUENT".equals(algorithm)) {
                nextTransitions = transitionDao.findByWordIdWeighted(currentWordId);
            } else {
                nextTransitions = transitionDao.findByWordId(currentWordId);
            }

            if (nextTransitions.isEmpty()) break;

            Transition chosen;
            if ("MOST_FREQUENT".equals(algorithm)) {
                // Always pick the most frequent next word
                chosen = nextTransitions.get(0);
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

            Word nextWord = wordDao.findById(chosen.getNextWordId());
            if (nextWord == null) break;

            sentence.append(" ").append(nextWord.getWord());
            currentWordId = nextWord.getWordId();
        }

        // Capitalize first letter and add period
        String result = sentence.toString();
        return result.substring(0, 1).toUpperCase() + result.substring(1) + ".";
    }
}
