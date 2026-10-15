package com.github.roleplaycauldron.brotkrumen.editor;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Configuration of the editor wand.
 *
 * @param material           item material of the wand
 * @param giveOnSessionStart whether a wand is given when an editor session starts
 * @param offsetFromSideFace whether clicks on a block face place the node next to that face instead of on top
 * @param soundFeedback      whether wand clicks play a confirmation sound
 * @param particleFeedback   whether wand clicks spawn confirmation particles
 * @param defaultEdgeType    edge type used when the wand connects two nodes
 * @param autoPlacementWhileHeld whether walking still places nodes automatically while the wand is held
 */
public record WandSettings(Material material, boolean giveOnSessionStart, boolean offsetFromSideFace,
                           boolean soundFeedback, boolean particleFeedback,
                           EditorService.EdgeType defaultEdgeType, boolean autoPlacementWhileHeld) {

    private static final Material FALLBACK_MATERIAL = Material.BREAD;

    /**
     * Returns the default settings: bread wand, given on session start, silent.
     *
     * @return default settings
     */
    public static WandSettings defaults() {
        return new WandSettings(FALLBACK_MATERIAL, true, false, false, false, EditorService.EdgeType.UNDIRECTED, false);
    }

    /**
     * Reads wand settings from the plugin configuration, falling back to defaults for missing or invalid values.
     *
     * @param config plugin configuration
     * @return wand settings
     */
    public static WandSettings fromConfig(final FileConfiguration config) {
        return fromConfig(config, message -> {
        });
    }

    /**
     * Reads wand settings from the plugin configuration, reporting replaced invalid values to the given sink.
     *
     * @param config  plugin configuration
     * @param warning receives a message for every value that was replaced by its default
     * @return wand settings
     */
    public static WandSettings fromConfig(final FileConfiguration config, final Consumer<String> warning) {
        final String configured = config.getString("wand.material", FALLBACK_MATERIAL.name());
        Material material = parseMaterial(configured);
        if (material == null) {
            warning.accept("Invalid wand.material '" + configured + "'; using " + FALLBACK_MATERIAL.name()
                    + ". The wand must be a non-placeable, non-tool item.");
            material = FALLBACK_MATERIAL;
        }
        return new WandSettings(material,
                config.getBoolean("wand.giveOnSessionStart", true),
                config.getBoolean("wand.offsetFromSideFace", false),
                config.getBoolean("wand.feedback.sounds", false),
                config.getBoolean("wand.feedback.particles", false),
                EditorService.EdgeType.parse(config.getString("wand.defaultEdgeType", "undirected"))
                        .orElse(EditorService.EdgeType.UNDIRECTED),
                config.getBoolean("wand.autoPlacementWhileHeld", false));
    }

    private static Material parseMaterial(final String value) {
        final Material parsed = value == null ? null : Material.matchMaterial(value.trim().toUpperCase(Locale.ROOT));
        if (parsed == null || !parsed.isItem() || parsed.isAir() || parsed.isBlock()
                || parsed.getMaxDurability() > 0 || parsed.name().endsWith("_BUCKET")) {
            return null;
        }
        return parsed;
    }
}
