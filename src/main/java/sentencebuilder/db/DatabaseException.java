/******************************************************************************
 * DatabaseException.java
 *
 * Author: James Human
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Unchecked exception wrapping SQLException. Allows the service layer to
 * handle database errors without being forced to catch checked exceptions.
 ******************************************************************************/
package sentencebuilder.db;

public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
