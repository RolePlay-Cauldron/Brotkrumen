package com.github.roleplaycauldron.brotkrumen.editor;

/**
 * Edge property the wand changes in Thread mode.
 */
public enum WandProperty {

    /**
     * Directed or undirected edge type.
     */
    TYPE("type"),

    /**
     * Open or blocked edge state.
     */
    STATE("state"),

    /**
     * Normal or teleport traversal.
     */
    TRAVERSAL("traversal");

    private final String serializedValue;

    WandProperty(final String configValue) {
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
     * Returns the next property, wrapping around after the last one.
     *
     * @return next property
     */
    public WandProperty next() {
        final WandProperty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
