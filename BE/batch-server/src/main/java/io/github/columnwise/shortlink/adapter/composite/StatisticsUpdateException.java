package io.github.columnwise.shortlink.adapter.composite;

/**
 * Statistics update operation failed exception
 */
public class StatisticsUpdateException extends RuntimeException {
    
    public StatisticsUpdateException(String message) {
        super(message);
    }
    
    public StatisticsUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}