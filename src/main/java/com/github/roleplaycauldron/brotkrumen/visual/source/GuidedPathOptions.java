package com.github.roleplaycauldron.brotkrumen.visual.source;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Tunable options for guided path visualization.
 *
 * @param windowSize       number of forward path nodes to expose
 * @param activationRadius distance at which a node is considered reached
 * @param lookBehind       number of previous nodes kept visible
 */
public record GuidedPathOptions(int windowSize, double activationRadius, int lookBehind) {

    private static final int DEFAULT_WINDOW_SIZE = 4;

    private static final double DEFAULT_ACTIVATION_RADIUS = 4.0D;

    private static final int DEFAULT_LOOK_BEHIND = 1;

    /**
     * Creates options and normalizes invalid values.
     *
     * @param windowSize       number of forward path nodes to expose
     * @param activationRadius distance at which a node is considered reached
     * @param lookBehind       number of previous nodes kept visible
     */
    public GuidedPathOptions {
        windowSize = Math.max(1, windowSize);
        activationRadius = Math.max(0.0D, activationRadius);
        lookBehind = Math.max(0, lookBehind);
    }

    /**
     * Gets built-in fallback defaults.
     *
     * @return default options
     */
    public static GuidedPathOptions defaults() {
        return new GuidedPathOptions(DEFAULT_WINDOW_SIZE, DEFAULT_ACTIVATION_RADIUS, DEFAULT_LOOK_BEHIND);
    }

    /**
     * Loads options from configuration, using built-in defaults for missing values.
     *
     * @param section configuration section
     * @return loaded options
     */
    public static GuidedPathOptions fromConfig(final ConfigurationSection section) {
        final GuidedPathOptions defaults = defaults();
        if (section == null) {
            return defaults;
        }
        return new GuidedPathOptions(
                section.getInt("windowSize", defaults.windowSize()),
                section.getDouble("activationRadius", defaults.activationRadius()),
                section.getInt("lookBehind", defaults.lookBehind())
        );
    }
}
