package com.github.roleplaycauldron.brotkrumen.visual.design;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Visualizer renderer and preset defaults loaded from plugin configuration.
 *
 * @param defaultRenderer              default renderer
 * @param defaultSpellbookEffectPreset fallback Spellbook effect preset
 * @param defaultBlockDisplayPreset    fallback block-display preset
 */
public record VisualizerRenderSettings(VisualRenderer defaultRenderer,
                                       String defaultSpellbookEffectPreset,
                                       String defaultBlockDisplayPreset) {

    private static final String DEFAULT_RENDERER = "visualizer.defaultRenderer";

    private static final String DEFAULT_SPELLBOOK_EFFECT_PRESET = "visualizer.defaultSpellbookEffectPreset";

    private static final String DEFAULT_BLOCK_DISPLAY_PRESET = "visualizer.defaultBlockDisplayPreset";

    private static final String FALLBACK_PRESET = "ember";

    /**
     * Creates normalized settings.
     */
    public VisualizerRenderSettings {
        if (defaultRenderer == null) {
            defaultRenderer = VisualRenderer.SPELLBOOK_EFFECT;
        }
        defaultSpellbookEffectPreset = normalizePreset(defaultSpellbookEffectPreset);
        defaultBlockDisplayPreset = normalizePreset(defaultBlockDisplayPreset);
    }

    /**
     * Loads settings from plugin config.
     *
     * @param config config
     * @return render settings
     */
    public static VisualizerRenderSettings fromConfig(final FileConfiguration config) {
        if (config == null) {
            return defaults();
        }
        return new VisualizerRenderSettings(
                VisualRenderer.parse(config.getString(DEFAULT_RENDERER, VisualRenderer.SPELLBOOK_EFFECT.configValue()))
                        .orElse(VisualRenderer.SPELLBOOK_EFFECT),
                config.getString(DEFAULT_SPELLBOOK_EFFECT_PRESET, FALLBACK_PRESET),
                config.getString(DEFAULT_BLOCK_DISPLAY_PRESET, FALLBACK_PRESET)
        );
    }

    /**
     * Returns default settings.
     *
     * @return defaults
     */
    public static VisualizerRenderSettings defaults() {
        return new VisualizerRenderSettings(VisualRenderer.SPELLBOOK_EFFECT, FALLBACK_PRESET, FALLBACK_PRESET);
    }

    private static String normalizePreset(final String preset) {
        final String normalized = VisualPresetRegistry.normalizePresetName(preset);
        return normalized.isBlank() ? FALLBACK_PRESET : normalized;
    }

    /**
     * Returns the configured preset for a renderer.
     *
     * @param renderer renderer
     * @return preset name
     */
    public String defaultPreset(final VisualRenderer renderer) {
        return switch (renderer) {
            case SPELLBOOK_EFFECT -> defaultSpellbookEffectPreset;
            case BLOCK_DISPLAY -> defaultBlockDisplayPreset;
        };
    }
}
