package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for cancelling guided resolve when the player leaves the route.
 *
 * @param enabled             whether away-cancellation is enabled
 * @param distance            maximum distance from the guided route
 * @param warningEnabled      whether leaving range sends a warning
 * @param warningGraceSeconds grace period before cancellation
 */
public record ResolveAwayCancellationOptions(boolean enabled, double distance, boolean warningEnabled,
                                             int warningGraceSeconds) {

    private static final boolean DEFAULT_ENABLED = true;

    private static final double DEFAULT_DISTANCE = 10.0D;

    private static final boolean DEFAULT_WARNING_ENABLED = true;

    private static final int DEFAULT_WARNING_GRACE_SECONDS = 2;

    /**
     * Normalizes invalid values.
     */
    public ResolveAwayCancellationOptions {
        distance = Math.max(0.0D, distance);
        warningGraceSeconds = Math.max(0, warningGraceSeconds);
    }

    /**
     * Built-in defaults.
     *
     * @return default options
     */
    public static ResolveAwayCancellationOptions defaults() {
        return new ResolveAwayCancellationOptions(DEFAULT_ENABLED, DEFAULT_DISTANCE, DEFAULT_WARNING_ENABLED,
                DEFAULT_WARNING_GRACE_SECONDS);
    }

    /**
     * Loads options from config.
     *
     * @param section config section
     * @return loaded options
     */
    public static ResolveAwayCancellationOptions fromConfig(final ConfigurationSection section) {
        final ResolveAwayCancellationOptions defaults = defaults();
        if (section == null) {
            return defaults;
        }
        return new ResolveAwayCancellationOptions(
                section.getBoolean("enabled", defaults.enabled()),
                section.getDouble("distance", defaults.distance()),
                section.getBoolean("warningEnabled", defaults.warningEnabled()),
                section.getInt("warningGraceSeconds", defaults.warningGraceSeconds())
        );
    }

    /**
     * Grace period converted to ticks.
     *
     * @return grace period ticks
     */
    public long warningGraceTicks() {
        return warningGraceSeconds * 20L;
    }
}
