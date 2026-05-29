package com.github.roleplaycauldron.brotkrumen.command.bk;

import java.util.Locale;

/**
 * Visualizer backend for resolve output.
 */
public enum ResolveBackend {
    /**
     * Spellbook particle backend.
     */
    PARTICLE,

    /**
     * Block-display backend.
     */
    BLOCK_DISPLAY;

    /**
     * Parses a configured backend.
     *
     * @param raw raw value
     * @return parsed backend, defaulting to particle
     */
    public static ResolveBackend parse(final String raw) {
        if (raw == null) {
            return PARTICLE;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("block-display".equals(normalized) || "block_display".equals(normalized)
                || "blockdisplay".equals(normalized)) {
            return BLOCK_DISPLAY;
        }
        return PARTICLE;
    }
}
