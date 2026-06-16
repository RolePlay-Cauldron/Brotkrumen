package com.github.roleplaycauldron.brotkrumen.visual.design;

import java.util.Locale;
import java.util.Optional;

/**
 * Supported graph visualization renderer families.
 */
public enum VisualRenderer {
    /**
     * Spellbook effect based particle renderer.
     */
    SPELLBOOK_EFFECT("spellbookEffect"),

    /**
     * Block-display entity renderer.
     */
    BLOCK_DISPLAY("blockDisplay");

    private final String serializedName;

    VisualRenderer(final String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Parses a renderer from config or command text.
     *
     * @param input renderer input
     * @return parsed renderer
     */
    public static Optional<VisualRenderer> parse(final String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        final String normalized = input.trim().replace('-', '_').toLowerCase(Locale.ROOT);
        for (final VisualRenderer renderer : values()) {
            if (renderer.serializedName.toLowerCase(Locale.ROOT).equals(normalized)
                    || renderer.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(renderer);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the config representation.
     *
     * @return config value
     */
    public String configValue() {
        return serializedName;
    }
}
