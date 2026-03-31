/******************************************************************************
 * Word.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
 *
 * Model class mapping to the 'words' table. Represents a unique word with
 * frequency counts for total occurrences, sentence starts, and sentence ends.
 ******************************************************************************/
package sentencebuilder.db.model;

import java.time.LocalDateTime;

public class Word {
    private int wordId;
    private String word;
    private int totalCount;
    private int startCount;
    private int endCount;
    private boolean userAdded;
    private LocalDateTime addedAt;

    public Word() {}

    public Word(String word, int totalCount, int startCount, int endCount) {
        this.word = word;
        this.totalCount = totalCount;
        this.startCount = startCount;
        this.endCount = endCount;
    }

    public int getWordId() { return wordId; }
    public void setWordId(int wordId) { this.wordId = wordId; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getStartCount() { return startCount; }
    public void setStartCount(int startCount) { this.startCount = startCount; }
    public int getEndCount() { return endCount; }
    public void setEndCount(int endCount) { this.endCount = endCount; }
    public boolean isUserAdded() { return userAdded; }
    public void setUserAdded(boolean userAdded) { this.userAdded = userAdded; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
