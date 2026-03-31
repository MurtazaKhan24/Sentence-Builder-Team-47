/******************************************************************************
 * GeneratedSentence.java
 *
 * Author: Pranava (schema design)
 * Revised by: James Human (Java implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Model class mapping to the 'generated_sentences' table. Stores each
 * sentence produced by the generation algorithms, along with metadata
 * about which algorithm and seed word were used.
 ******************************************************************************/
package sentencebuilder.db.model;

import java.time.LocalDateTime;

public class GeneratedSentence {
    private int sentenceId;
    private String sentenceText;
    private Integer seedWordId;
    private String algorithm;
    private int wordCount;
    private LocalDateTime generatedAt;

    public GeneratedSentence() {}

    public GeneratedSentence(String sentenceText, Integer seedWordId,
                             String algorithm, int wordCount) {
        this.sentenceText = sentenceText;
        this.seedWordId = seedWordId;
        this.algorithm = algorithm;
        this.wordCount = wordCount;
    }

    public int getSentenceId() { return sentenceId; }
    public void setSentenceId(int sentenceId) { this.sentenceId = sentenceId; }
    public String getSentenceText() { return sentenceText; }
    public void setSentenceText(String sentenceText) { this.sentenceText = sentenceText; }
    public Integer getSeedWordId() { return seedWordId; }
    public void setSeedWordId(Integer seedWordId) { this.seedWordId = seedWordId; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
