/******************************************************************************
 * Occurrence.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
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
