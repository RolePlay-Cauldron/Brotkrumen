package com.github.roleplaycauldron.brotkrumen.storage;

/**
 * Represents an exception that occurs during storage-related operations.
 * <p>
 * This exception is typically thrown when an error is encountered while working
 * with a storage system, such as a database or file-based storage.
 * <p>
 * It extends the {@link RuntimeException}, allowing it to be used for unchecked
 * exceptions that propagate runtime errors.
 */
public class StorageException extends RuntimeException {

    /**
     * Constructs a new StorageException with the specified detail message.
     *
     * @param message the detail message to be included in the exception
     */
    public StorageException(final String message) {
        super(message);
    }

    /**
     * Constructs a new StorageException with the specified detail message and cause.
     *
     * @param message the detail message to be included in the exception
     * @param cause   the underlying cause of the exception, typically another exception
     */
    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
