package com.github.roleplaycauldron.brotkrumen.command.bk;

import java.io.Serial;
import java.util.Map;

/**
 * Represents an exception that occurs during target parsing operations.
 * <p>
 * This exception is typically thrown when an error is encountered while parsing
 * flexible {@code /bk resolve} target tokens.
 * <p>
 * It extends the {@link Exception}, allowing it to be used for checked
 * exceptions that must be handled by the caller.
 */
public class TargetParseException extends Exception {

    @Serial
    private static final long serialVersionUID = -5099275603651221284L;

    private final String errorKey;

    private final Map<String, String> replacements;

    /**
     * Constructs a new TargetParseException with the specified localization key.
     *
     * @param errorKey the localization key to be included in the exception
     */
    public TargetParseException(final String errorKey) {
        this(errorKey, Map.of());
    }

    /**
     * Constructs a new TargetParseException with the specified localization key and replacements.
     *
     * @param errorKey     the localization key to be included in the exception
     * @param replacements message replacements
     */
    public TargetParseException(final String errorKey, final Map<String, String> replacements) {
        super(errorKey);
        this.errorKey = errorKey;
        this.replacements = Map.copyOf(replacements);
    }

    /**
     * Gets the localization key.
     *
     * @return error key
     */
    public String getErrorKey() {
        return errorKey;
    }

    /**
     * Gets the message replacements.
     *
     * @return replacements
     */
    public Map<String, String> getReplacements() {
        return replacements;
    }
}
