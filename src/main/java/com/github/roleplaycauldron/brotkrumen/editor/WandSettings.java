package com.github.roleplaycauldron.brotkrumen.editor;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Configuration of the editor wand.
 *
 * @param material           item material of the wand
 * @param giveOnSessionStart whether a wand is given when an editor session starts
 * @param offsetFromSideFace whether clicks on a block face place the node next to that face instead of on top
 * @param soundFeedback      whether wand clicks play a confirmation sound
 * @param particleFeedback   whether wand clicks spawn confirmation particles
 * @param defaultEdgeType    edge type used when the wand connects two nodes
 */
public record WandSettings(Material material, boolean giveOnSessionStart, boolean offsetFromSideFace,
                           boolean soundFeedback, boolean particleFeedback,
                           EditorService.EdgeType defaultEdgeType) {

    private static final Material FALLBACK_MATERIAL = Material.BREAD;

    /**
     * Returns the default settings: bread wand, given on session start, silent.
     *
     * @return default settings
     */
    public static WandSettings defaults() {
        return new WandSettings(FALLBACK_MATERIAL, true, false, false, false, EditorService.EdgeType.UNDIRECTED);
    }

    /**
     * Reads wand settings from the plugin configuration, falling back to defaults for missing or invalid values.
     *
     * @param config plugin configuration
     * @return wand settings
     */
    public static WandSettings fromConfig(final FileConfiguration config) {
        final Material material = parseMaterial(config.getString("wand.material", FALLBACK_MATERIAL.name()));
        return new WandSettings(material,
                config.getBoolean("wand.giveOnSessionStart", true),
                config.getBoolean("wand.offsetFromSideFace", false),
                config.getBoolean("wand.feedback.sounds", false),
                config.getBoolean("wand.feedback.particles", false),
                EditorService.EdgeType.parse(config.getString("wand.defaultEdgeType", "undirected"))
                        .orElse(EditorService.EdgeType.UNDIRECTED));
    }

    private static Material parseMaterial(final String value) {
        final Material parsed = value == null ? null : Material.matchMaterial(value.trim().toUpperCase(Locale.ROOT));
        return parsed == null || !parsed.isItem() || parsed.isAir() ? FALLBACK_MATERIAL : parsed;
    }
}
