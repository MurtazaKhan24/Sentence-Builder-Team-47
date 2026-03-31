/******************************************************************************
 * ImportedFile.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (Java implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Model class mapping to the 'imported_files' table. Tracks metadata about
 * each text file that has been imported into the system.
 ******************************************************************************/
package sentencebuilder.db.model;

import java.time.LocalDateTime;

public class ImportedFile {
    private int fileId;
    private String filename;
    private String filepath;
    private int wordCount;
    private int sentenceCount;
    private LocalDateTime importedAt;
    private String notes;

    public ImportedFile() {}

    public ImportedFile(String filename, String filepath) {
        this.filename = filename;
        this.filepath = filepath;
    }

    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getFilepath() { return filepath; }
    public void setFilepath(String filepath) { this.filepath = filepath; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public int getSentenceCount() { return sentenceCount; }
    public void setSentenceCount(int sentenceCount) { this.sentenceCount = sentenceCount; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
