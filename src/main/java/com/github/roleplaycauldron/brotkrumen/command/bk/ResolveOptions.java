package com.github.roleplaycauldron.brotkrumen.command.bk;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration for `/bk resolve`.
 *
 * @param nearestNodeRadius configured radius before clamping
 * @param viewDistance      visualizer view distance
 * @param backend           configured visualizer backend
 */
public record ResolveOptions(double nearestNodeRadius, double viewDistance, ResolveBackend backend) {

    private static final String NEAREST_NODE_RADIUS = "commands.resolve.nearestNodeRadius";

    private static final String VISUALIZER_BACKEND = "commands.resolve.visualizerBackend";

    private static final String VIEW_DISTANCE = "visualizer.viewDistance";

    private static final double DEFAULT_NEAREST_NODE_RADIUS = 15.0D;

    private static final double DEFAULT_VIEW_DISTANCE = 16.0D;

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
                ResolveBackend.parse(config.getString(VISUALIZER_BACKEND, "particle"))
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
}
