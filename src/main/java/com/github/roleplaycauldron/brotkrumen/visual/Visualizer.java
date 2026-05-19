package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.GraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.VisualGraphSource;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Source-backed visualization orchestrator for graph, network, path, and guided-path snapshots.
 */
@SuppressWarnings("PMD.ShortVariable")
public class Visualizer {

    /**
     * The loggerFactory for this class.
     */
    protected final LoggerFactory loggerFactory;

    /**
     * The legacy graph used by direct graph helper methods.
     */
    protected final Graph graph;

    private final VisualGraphSource source;

    private final GraphRenderer renderer;

    private final GraphDesignResolver designs;

    private long lastRenderedVersion = Long.MIN_VALUE;

    /**
     * Constructs a legacy graph-bound visualizer.
     *
     * @param loggerFactory the factory to create loggers for this instance
     * @param graph         the graph structure to be visualized
     */
    public Visualizer(final LoggerFactory loggerFactory, final Graph graph) {
        this.loggerFactory = loggerFactory;
        this.graph = graph;
        this.source = null;
        this.renderer = null;
        this.designs = null;
    }

    /**
     * Constructs a source-backed visualizer.
     *
     * @param loggerFactory the factory to create loggers for this instance
     * @param source        visual graph source
     * @param renderer      renderer
     * @param designs       design resolver
     */
    public Visualizer(final LoggerFactory loggerFactory, final VisualGraphSource source,
                      final GraphRenderer renderer, final GraphDesignResolver designs) {
        this.loggerFactory = loggerFactory;
        this.graph = null;
        this.source = source;
        this.renderer = renderer;
        this.designs = designs;
    }

    /**
     * Releases rendered resources owned by this visualizer.
     */
    public void shutdown() {
        if (renderer != null) {
            renderer.shutdown();
        }
    }

    /**
     * Reconciles the visualizer with the latest source snapshot.
     */
    public void refresh() {
        lastRenderedVersion = Long.MIN_VALUE;
        visibilityUpdate();
    }

    /**
     * Updates rendered state or viewer-only visibility for the current source snapshot.
     */
    /* default */
    void visibilityUpdate() {
        if (source == null || renderer == null || designs == null) {
            return;
        }
        if (source.version() == lastRenderedVersion) {
            renderer.applyVisibilityOnly();
            return;
        }
        renderer.apply(source.snapshot(), designs);
        lastRenderedVersion = source.version();
    }

    /**
     * Calculates the squared distance from a point to the center of a specified node.
     *
     * @param x    the x-coordinate of the point
     * @param y    the y-coordinate of the point
     * @param z    the z-coordinate of the point
     * @param node the node whose center is used for distance calculation
     * @return squared distance between the point and the node center
     */
    protected double nodeDistanceSquared(final double x, final double y, final double z, final Node node) {
        final double dx = x - (node.x() + 0.5D);
        final double dy = y - (node.y() + 0.5D);
        final double dz = z - (node.z() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Retrieves node identifiers within a squared radius from a location.
     *
     * @param location search center
     * @param radiusSq squared radius
     * @return graph IDs of nodes within range
     */
    protected Set<UUID> nodesWithin(final Location location, final double radiusSq) {
        final double xLoc = location.getX();
        final double yLoc = location.getY();
        final double zLoc = location.getZ();
        final World world = location.getWorld();

        final Set<UUID> out = new HashSet<>();
        final UUID wid = world.getUID();
        for (final Node node : graph.getNodes()) {
            if (!node.worldId().equals(wid)) {
                continue;
            }
            if (nodeDistanceSquared(xLoc, yLoc, zLoc, node) <= radiusSq) {
                out.add(node.graphId());
            }
        }
        return out;
    }

    /**
     * Calculates squared spawn radius from view distance and buffer.
     *
     * @param viewDistance visible distance
     * @param buffer       additional spawn buffer
     * @return squared spawn radius
     */
    protected double spawnRadiusSquared(final double viewDistance, final double buffer) {
        final double radius = viewDistance + buffer;
        return radius * radius;
    }
}
