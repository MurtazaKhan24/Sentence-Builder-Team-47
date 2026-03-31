/******************************************************************************
 * Occurrence.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (Java implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Model class mapping to the 'word_file_occurrences' table. Tracks how
 * many times a specific word appears in a specific imported file.
 ******************************************************************************/
package sentencebuilder.db.model;

public class Occurrence {
    private int wordId;
    private int fileId;
    private int count;

    public Occurrence() {}

    public Occurrence(int wordId, int fileId, int count) {
        this.wordId = wordId;
        this.fileId = fileId;
        this.count = count;
    }

    public int getWordId() { return wordId; }
    public void setWordId(int wordId) { this.wordId = wordId; }
    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
