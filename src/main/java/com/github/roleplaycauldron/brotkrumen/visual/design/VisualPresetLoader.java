package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.effect.config.EffectConfigParser;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and validates file-backed visual presets.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.TooManyMethods",
        "PMD.ExceptionAsFlowControl", "PMD.AvoidCatchingGenericException"})
public final class VisualPresetLoader {

    private static final String PRESETS_RESOURCE = "presets.yml";

    private static final String PARTICLE_SECTION = "particle";

    private static final String BLOCK_DISPLAY_SECTION = "block-display";

    private static final String NODES_SECTION = "nodes";

    private static final String EDGES_SECTION = "edges";

    private final JavaPlugin plugin;

    private final WrappedLogger log;

    private final EffectConfigParser effectParser = EffectConfigParser.defaults();

    /**
     * Creates a loader.
     *
     * @param plugin plugin
     * @param log    logger
     */
    public VisualPresetLoader(final JavaPlugin plugin, final WrappedLogger log) {
        this.plugin = plugin;
        this.log = log;
    }

    /**
     * Loads presets for startup and fails if no valid preset can be loaded.
     *
     * @return loaded registry
     */
    public VisualPresetRegistry loadForStartup() {
        final VisualPresetRegistry registry = loadFromDataFolder();
        if (registry.isEmpty()) {
            throw new VisualPresetLoadException("No valid visual presets could be loaded from presets.yml.");
        }
        return registry;
    }

    /**
     * Reloads presets, retaining the current cache if reload produces no valid entries.
     *
     * @param current current registry
     * @return reloaded registry or current registry
     */
    public VisualPresetRegistry reload(final VisualPresetRegistry current) {
        final VisualPresetRegistry registry = loadFromDataFolder();
        if (registry.isEmpty()) {
            error("Preset reload failed: no valid presets were loaded. Keeping the previous preset cache.");
            return current;
        }
        return registry;
    }

    /**
     * Parses presets from a configuration section.
     *
     * @param root root section
     * @return parsed registry
     */
    public VisualPresetRegistry parse(final ConfigurationSection root) {
        final Map<String, VisualPreset> presets = new LinkedHashMap<>();
        for (final String key : root.getKeys(false)) {
            final String path = key;
            final String normalized = VisualPresetRegistry.normalizePresetName(key);
            if (normalized.isBlank()) {
                error("Skipping visual preset '" + path + "': preset name must not be blank.");
                continue;
            }
            if (presets.containsKey(normalized)) {
                error("Skipping visual preset '" + path + "': duplicate normalized name '" + normalized + "'.");
                continue;
            }

            try {
                final ConfigurationSection presetSection = requiredSection(root, key, path);
                validatePresetSections(presetSection, path);
                final VisualPreset preset = new VisualPreset(normalized,
                        parseParticleSet(optionalSection(presetSection, PARTICLE_SECTION), path + "." + PARTICLE_SECTION),
                        parseBlockDisplaySet(optionalSection(presetSection, BLOCK_DISPLAY_SECTION),
                                path + "." + BLOCK_DISPLAY_SECTION));
                if (!preset.supports(VisualRenderer.SPELLBOOK_EFFECT) && !preset.supports(VisualRenderer.BLOCK_DISPLAY)) {
                    throw new IllegalArgumentException("Preset must define particle or block-display data.");
                }
                presets.put(normalized, preset);
            } catch (final RuntimeException failure) {
                error("Skipping visual preset '" + path + "': " + failure.getMessage());
            }
        }
        return new VisualPresetRegistry(presets);
    }

    private VisualPresetRegistry loadFromDataFolder() {
        final File file = new File(plugin.getDataFolder(), PRESETS_RESOURCE);
        if (!file.isFile()) {
            plugin.saveResource(PRESETS_RESOURCE, false);
        }
        return parse(YamlConfiguration.loadConfiguration(file));
    }

    private void validatePresetSections(final ConfigurationSection section, final String path) {
        for (final String key : section.getKeys(false)) {
            if (!PARTICLE_SECTION.equals(key) && !BLOCK_DISPLAY_SECTION.equals(key)) {
                throw new IllegalArgumentException("Unknown section '" + path + "." + key
                        + "'. Expected 'particle' or 'block-display'.");
            }
        }
    }

    private ParticleDesignSet parseParticleSet(final ConfigurationSection section, final String path) {
        if (section == null) {
            return null;
        }
        return new ParticleDesignSet(
                parseParticleNodes(requiredSection(section, NODES_SECTION, path + "." + NODES_SECTION),
                        path + "." + NODES_SECTION),
                parseParticleEdges(requiredSection(section, EDGES_SECTION, path + "." + EDGES_SECTION),
                        path + "." + EDGES_SECTION)
        );
    }

    private Map<VisualNodeRole, ParticleNodeDesign> parseParticleNodes(final ConfigurationSection section,
                                                                       final String path) {
        final Map<VisualNodeRole, ParticleNodeDesign> result = new EnumMap<>(VisualNodeRole.class);
        for (final String key : section.getKeys(false)) {
            final VisualNodeRole role = parseEnum(VisualNodeRole.class, key, path + "." + key);
            final ConfigurationSection design = requiredSection(section, key, path + "." + key);
            result.put(role, new ParticleNodeDesign(parseParticle(design, path + "." + key), effectParser.parse(design)));
        }
        requireRole(result, VisualNodeRole.DEFAULT, path);
        return result;
    }

    private Map<VisualEdgeRole, ParticleEdgeDesign> parseParticleEdges(final ConfigurationSection section,
                                                                       final String path) {
        final Map<VisualEdgeRole, ParticleEdgeDesign> result = new EnumMap<>(VisualEdgeRole.class);
        for (final String key : section.getKeys(false)) {
            final VisualEdgeRole role = parseEnum(VisualEdgeRole.class, key, path + "." + key);
            final ConfigurationSection design = requiredSection(section, key, path + "." + key);
            result.put(role, new ParticleEdgeDesign(parseParticle(design, path + "." + key), effectParser.parse(design)));
        }
        requireRole(result, VisualEdgeRole.DEFAULT_LOCAL, path);
        return result;
    }

    private Particle parseParticle(final ConfigurationSection section, final String path) {
        final ConfigurationSection particle = requiredSection(section, "particle", path + ".particle");
        final String type = particle.getString("type");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Missing particle type at '" + path + ".particle.type'.");
        }
        return parseEnum(Particle.class, type, path + ".particle.type");
    }

    private BlockDisplayDesignSet parseBlockDisplaySet(final ConfigurationSection section, final String path) {
        if (section == null) {
            return null;
        }
        return new BlockDisplayDesignSet(
                parseBlockNodes(requiredSection(section, NODES_SECTION, path + "." + NODES_SECTION),
                        path + "." + NODES_SECTION),
                parseBlockEdges(requiredSection(section, EDGES_SECTION, path + "." + EDGES_SECTION),
                        path + "." + EDGES_SECTION)
        );
    }

    private Map<VisualNodeRole, BlockNodeDesign> parseBlockNodes(final ConfigurationSection section,
                                                                 final String path) {
        final Map<VisualNodeRole, BlockNodeDesign> result = new EnumMap<>(VisualNodeRole.class);
        for (final String key : section.getKeys(false)) {
            final VisualNodeRole role = parseEnum(VisualNodeRole.class, key, path + "." + key);
            final ConfigurationSection design = requiredSection(section, key, path + "." + key);
            result.put(role, new BlockNodeDesign(parseMaterial(design, path + "." + key),
                    requireFloat(design, "scale", path + "." + key + ".scale")));
        }
        requireRole(result, VisualNodeRole.DEFAULT, path);
        return result;
    }

    private Map<VisualEdgeRole, BlockEdgeDesign> parseBlockEdges(final ConfigurationSection section,
                                                                 final String path) {
        final Map<VisualEdgeRole, BlockEdgeDesign> result = new EnumMap<>(VisualEdgeRole.class);
        for (final String key : section.getKeys(false)) {
            final VisualEdgeRole role = parseEnum(VisualEdgeRole.class, key, path + "." + key);
            final ConfigurationSection design = requiredSection(section, key, path + "." + key);
            result.put(role, new BlockEdgeDesign(parseMaterial(design, path + "." + key),
                    requireFloat(design, "thickness", path + "." + key + ".thickness"),
                    requireDouble(design, "node-clearance", path + "." + key + ".node-clearance")));
        }
        requireRole(result, VisualEdgeRole.DEFAULT_LOCAL, path);
        return result;
    }

    private Material parseMaterial(final ConfigurationSection section, final String path) {
        final String material = section.getString("material");
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("Missing material at '" + path + ".material'.");
        }
        return parseEnum(Material.class, material, path + ".material");
    }

    private ConfigurationSection optionalSection(final ConfigurationSection section, final String key) {
        return section.getConfigurationSection(key);
    }

    private ConfigurationSection requiredSection(final ConfigurationSection section, final String key,
                                                 final String path) {
        final ConfigurationSection child = section.getConfigurationSection(key);
        if (child == null) {
            throw new IllegalArgumentException("Missing section '" + path + "'.");
        }
        return child;
    }

    private float requireFloat(final ConfigurationSection section, final String key, final String path) {
        if (!section.isSet(key)) {
            throw new IllegalArgumentException("Missing number at '" + path + "'.");
        }
        return (float) section.getDouble(key);
    }

    private double requireDouble(final ConfigurationSection section, final String key, final String path) {
        if (!section.isSet(key)) {
            throw new IllegalArgumentException("Missing number at '" + path + "'.");
        }
        return section.getDouble(key);
    }

    private <E extends Enum<E>> E parseEnum(final Class<E> type, final String value, final String path) {
        final String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized);
        } catch (final IllegalArgumentException failure) {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName()
                    + " value '" + value + "' at '" + path + "'.", failure);
        }
    }

    private <K> void requireRole(final Map<K, ?> designs, final K role, final String path) {
        if (!designs.containsKey(role)) {
            throw new IllegalArgumentException("Missing required role '" + role + "' at '" + path + "'.");
        }
    }

    private void error(final String message) {
        if (log != null) {
            log.error(message);
        }
    }
}
