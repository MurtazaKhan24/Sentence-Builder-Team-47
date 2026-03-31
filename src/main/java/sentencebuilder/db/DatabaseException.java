/******************************************************************************
 * DatabaseException.java
 *
 * Written by James for CS4485, Senior Design Project, starting Mar 2026.
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
