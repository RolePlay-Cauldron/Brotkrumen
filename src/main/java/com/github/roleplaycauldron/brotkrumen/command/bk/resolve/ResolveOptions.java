package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration for `/bk resolve`.
 *
 * @param nearestNodeRadius         configured radius before clamping
 * @param viewDistance              visualizer view distance
 * @param backend                   configured visualizer backend
 * @param finishRadius              completion radius for guided resolve finish detection
 * @param finishCleanupDelaySeconds delay after completion before resolve guidance cleanup
 * @param goalMarkerEnabled         whether guided resolve highlights the final goal node
 * @param teleportRules             teleport rules
 * @param autoTeleportOptions       automatic teleport options
 */
public record ResolveOptions(double nearestNodeRadius, double viewDistance, ResolveBackend backend,
                             double finishRadius, int finishCleanupDelaySeconds, boolean goalMarkerEnabled,
                             String teleportRules, ResolveAutoTeleportOptions autoTeleportOptions) {

    private static final String NEAREST_NODE_RADIUS = "commands.resolve.nearestNodeRadius";

    private static final String VISUALIZER_BACKEND = "commands.resolve.visualizerBackend";

    private static final String FINISH_RADIUS = "commands.resolve.finishRadius";

    private static final String FINISH_CLEANUP_DELAY = "commands.resolve.finishCleanupDelaySeconds";

    private static final String GOAL_MARKER_ENABLED = "commands.resolve.goalMarkerEnabled";

    private static final String TELEPORT_RULES = "commands.resolve.teleportRules";

    private static final String AUTO_TELEPORT = "commands.resolve.autoTeleport";

    private static final String VIEW_DISTANCE = "visualizer.viewDistance";

    private static final double DEFAULT_NEAREST_NODE_RADIUS = 15.0D;

    private static final double DEFAULT_VIEW_DISTANCE = 16.0D;

    private static final double DEFAULT_FINISH_RADIUS = 4.0D;

    private static final int DEFAULT_FINISH_CLEANUP_DELAY = 5;

    private static final boolean DEFAULT_GOAL_MARKER_ENABLED = true;

    private static final String DEFAULT_TELEPORT_RULES = "LOCAL_INTERGRAPH_WARP";

    /**
     * Normalizes invalid values.
     */
    public ResolveOptions {
        nearestNodeRadius = Math.max(0.0D, nearestNodeRadius);
        viewDistance = Math.max(0.0D, viewDistance);
        finishRadius = Math.max(0.0D, finishRadius);
        finishCleanupDelaySeconds = Math.max(0, finishCleanupDelaySeconds);
    }

    /**
     * Loads resolve options from configuration.
     *
     * @param config plugin config
     * @return resolve options
     */
    public static ResolveOptions fromConfig(final FileConfiguration config) {
        return new ResolveOptions(
                config.getDouble(NEAREST_NODE_RADIUS, DEFAULT_NEAREST_NODE_RADIUS),
                config.getDouble(VIEW_DISTANCE, DEFAULT_VIEW_DISTANCE),
                ResolveBackend.parse(config.getString(VISUALIZER_BACKEND, "particle")),
                config.getDouble(FINISH_RADIUS, DEFAULT_FINISH_RADIUS),
                config.getInt(FINISH_CLEANUP_DELAY, DEFAULT_FINISH_CLEANUP_DELAY),
                config.getBoolean(GOAL_MARKER_ENABLED, DEFAULT_GOAL_MARKER_ENABLED),
                config.getString(TELEPORT_RULES, DEFAULT_TELEPORT_RULES),
                ResolveAutoTeleportOptions.fromConfig(config.getConfigurationSection(AUTO_TELEPORT))
        );
    }

    /**
     * Clamps nearest-node radius to visualizer view distance.
     *
     * @param nearestNodeRadius configured radius
     * @param viewDistance      view distance
     * @return effective radius
     */
    public static double effectiveNearestNodeRadius(final double nearestNodeRadius, final double viewDistance) {
        return Math.max(0.0D, Math.min(nearestNodeRadius, viewDistance));
    }

    /**
     * Gets the clamped nearest-node radius.
     *
     * @return effective radius
     */
    public double effectiveNearestNodeRadius() {
        return effectiveNearestNodeRadius(nearestNodeRadius, viewDistance);
    }

    /**
     * Converts configured cleanup delay to scheduler ticks.
     *
     * @return cleanup delay in ticks
     */
    public long finishCleanupDelayTicks() {
        return finishCleanupDelaySeconds * 20L;
    }
}
