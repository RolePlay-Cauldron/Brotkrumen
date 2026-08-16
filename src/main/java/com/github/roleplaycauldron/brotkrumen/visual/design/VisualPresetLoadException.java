package com.github.roleplaycauldron.brotkrumen.visual.design;

import java.util.List;

/**
 * Raised when visual presets cannot be loaded or validated.
 */
public class VisualPresetLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception.
     *
     * @param message error message
     */
    public VisualPresetLoadException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the underlying cause.
     *
     * @param message error message
     * @param cause   underlying failure
     */
    public VisualPresetLoadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception that includes all validation failures from one load attempt.
     *
     * @param failures validation failures
     */
    public VisualPresetLoadException(final List<String> failures) {
        super("Invalid visual presets in presets.yml: " + String.join("; ", failures));
    }
}
