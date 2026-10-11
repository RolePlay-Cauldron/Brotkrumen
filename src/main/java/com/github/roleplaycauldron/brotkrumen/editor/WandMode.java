package com.github.roleplaycauldron.brotkrumen.editor;

/**
 * Interaction mode of the editor wand.
 */
public enum WandMode {

    /**
     * Places nodes on clicked blocks and selects nearby graph elements.
     */
    CRUMB("crumb"),

    /**
     * Selects nodes and edges and edits their connections.
     */
    THREAD("thread");

    private final String serializedValue;

    WandMode(final String configValue) {
        this.serializedValue = configValue;
    }

    /**
     * Returns the lowercase identifier used in localization keys.
     *
     * @return identifier
     */
    public String configValue() {
        return serializedValue;
    }

    /**
     * Returns the other mode.
     *
     * @return toggled mode
     */
    public WandMode toggled() {
        return this == CRUMB ? THREAD : CRUMB;
    }
}
