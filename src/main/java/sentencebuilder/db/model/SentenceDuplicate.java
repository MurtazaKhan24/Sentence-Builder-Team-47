/******************************************************************************
 * SentenceDuplicate.java
 * 
 * Author: Murtaza Khan
 * Date implemented: 4/29/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 * 
 * Model class representing a sentence that appears more than once in the
 * generated sentence history, along with its duplicate count.
 ******************************************************************************/
package sentencebuilder.db.model;

public class SentenceDuplicate {
    private String sentenceText;
    private int duplicateCount;

    public SentenceDuplicate() {}

    public SentenceDuplicate(String sentenceText, int duplicateCount) {
        this.sentenceText = sentenceText;
        this.duplicateCount = duplicateCount;
    }

    public String getSentenceText() { return sentenceText; }
    public void setSentenceText(String sentenceText) { this.sentenceText = sentenceText; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
}