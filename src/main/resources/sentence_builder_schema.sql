CREATE DATABASE IF NOT EXISTS sentence_builder;
USE sentence_builder;

CREATE TABLE imported_files (
    file_id        INT           AUTO_INCREMENT PRIMARY KEY,
    filename       VARCHAR(255)  NOT NULL,
    filepath       VARCHAR(500)  NOT NULL UNIQUE,
    word_count     INT           NOT NULL DEFAULT 0,
    sentence_count INT           NOT NULL DEFAULT 0,
    imported_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes          VARCHAR(500),
    CONSTRAINT chk_word_count     CHECK (word_count >= 0),
    CONSTRAINT chk_sentence_count CHECK (sentence_count >= 0)
);

CREATE TABLE words (
    word_id      INT           AUTO_INCREMENT PRIMARY KEY,
    word         VARCHAR(100)  NOT NULL UNIQUE,
    total_count  INT           NOT NULL DEFAULT 0,
    start_count  INT           NOT NULL DEFAULT 0,
    end_count    INT           NOT NULL DEFAULT 0,
    user_added   TINYINT(1)    NOT NULL DEFAULT 0,
    added_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_total_count CHECK (total_count >= 0),
    CONSTRAINT chk_start_count CHECK (start_count >= 0),
    CONSTRAINT chk_end_count   CHECK (end_count >= 0),
    CONSTRAINT chk_user_added  CHECK (user_added IN (0, 1))
);

CREATE INDEX idx_words_word ON words(word);

CREATE TABLE word_transitions (
    transition_id  INT  AUTO_INCREMENT PRIMARY KEY,
    word_id        INT  NOT NULL,
    next_word_id   INT  NOT NULL,
    count          INT  NOT NULL DEFAULT 1,
    CONSTRAINT fk_transition_word      FOREIGN KEY (word_id)      REFERENCES words(word_id) ON UPDATE CASCADE,
    CONSTRAINT fk_transition_next_word FOREIGN KEY (next_word_id) REFERENCES words(word_id) ON UPDATE CASCADE,
    CONSTRAINT uq_transition           UNIQUE (word_id, next_word_id),
    CONSTRAINT chk_transition_count    CHECK (count >= 1)
);

CREATE INDEX idx_transitions_word_id ON word_transitions(word_id);

CREATE TABLE word_file_occurrences (
    word_id  INT  NOT NULL,
    file_id  INT  NOT NULL,
    count    INT  NOT NULL DEFAULT 1,
    PRIMARY KEY (word_id, file_id),
    CONSTRAINT fk_occurrence_word FOREIGN KEY (word_id) REFERENCES words(word_id) ON UPDATE CASCADE,
    CONSTRAINT fk_occurrence_file FOREIGN KEY (file_id) REFERENCES imported_files(file_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_occurrence_count CHECK (count >= 1)
);

CREATE TABLE generated_sentences (
    sentence_id    INT           AUTO_INCREMENT PRIMARY KEY,
    sentence_text  TEXT          NOT NULL,
    seed_word_id   INT,
    algorithm      VARCHAR(50)   NOT NULL,
    word_count     INT           NOT NULL DEFAULT 0,
    generated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sentence_seed_word   FOREIGN KEY (seed_word_id) REFERENCES words(word_id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_sentence_word_count CHECK (word_count >= 0)
);

CREATE INDEX idx_sentences_seed_word ON generated_sentences(seed_word_id);
CREATE INDEX idx_sentences_algorithm  ON generated_sentences(algorithm);
