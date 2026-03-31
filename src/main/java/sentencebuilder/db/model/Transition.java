/******************************************************************************
 * Transition.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
 *
 * Model class mapping to the 'word_transitions' table. Represents a
 * directional link from one word to the next, with a frequency count.
 ******************************************************************************/
package sentencebuilder.db.model;

public class Transition {
    private int transitionId;
    private int wordId;
    private int nextWordId;
    private int count;

    public Transition() {}

    public Transition(int wordId, int nextWordId, int count) {
        this.wordId = wordId;
        this.nextWordId = nextWordId;
        this.count = count;
    }

    public int getTransitionId() { return transitionId; }
    public void setTransitionId(int transitionId) { this.transitionId = transitionId; }
    public int getWordId() { return wordId; }
    public void setWordId(int wordId) { this.wordId = wordId; }
    public int getNextWordId() { return nextWordId; }
    public void setNextWordId(int nextWordId) { this.nextWordId = nextWordId; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
