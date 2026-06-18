package com.github.roleplaycauldron.brotkrumen.visual.design;

/**
 * Raised when visual presets cannot be loaded for startup.
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
}
