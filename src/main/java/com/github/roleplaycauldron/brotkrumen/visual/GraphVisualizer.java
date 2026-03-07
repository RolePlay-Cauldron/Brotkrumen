package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An abstract class for visualizing graph structures, providing methods and utilities
 * to process, manage, and query graph nodes and edges. Handles spatial computations
 * and visibility updates for nodes and edges.
 */
@SuppressWarnings("PMD.ShortVariable")
public abstract class GraphVisualizer {

    /**
     * The loggerFactory for this class.
     */
    protected final LoggerFactory loggerFactory;

    /**
     * The Graph to visualize.
     */
    protected final Graph graph;

    /**
     * Constructs a new instance of the {@code GraphVisualiser} class for managing
     * and visualizing a graph structure.
     *
     * @param loggerFactory the factory to create loggers for this instance
     * @param graph         the graph structure to be visualized
     */
    public GraphVisualizer(final LoggerFactory loggerFactory, final Graph graph) {
        this.loggerFactory = loggerFactory;
        this.graph = graph;
    }

    /**
     * Terminates the operations of this {@code GraphVisualiser} instance and releases any associated resources.
     * This method is meant to be invoked when the visualizer is no longer needed or is being unregistered.
     * Implementations should ensure a clean shutdown process, including stopping any ongoing tasks
     * or freeing resources such as memory or listeners.
     */
    public abstract void shutdown();

    /**
     * Updates the visibility state of the graph or its elements managed by this {@code GraphVisualiser} instance.
     * <p>
     * This method is expected to handle tasks such as recalculating visibility conditions,
     * updating rendered elements, or performing cleanup of elements that should no longer
     * be displayed to maintain synchronization between the graph's state and its visual representation.
     * <p>
     * Implementations should ensure that this method operates efficiently to avoid
     * performance degradation in cases where frequent visibility updates are required.
     */
    /* default */
    abstract void visibilityUpdate();

    /**
     * Calculates the squared distance from a point to the center of a specified node.
     * <p>
     * The node's position is adjusted to its center by adding 0.5 to each of its
     * x, y, and z coordinates before performing the distance calculation. This method
     * avoids computing the square root for performance reasons, as the squared distance
     * is sufficient for many use cases such as comparisons.
     *
     * @param x    the x-coordinate of the point
     * @param y    the y-coordinate of the point
     * @param z    the z-coordinate of the point
     * @param node the {@code Node} whose center is used for distance calculation
     * @return the squared distance between the specified point and the center of the node
     */
    protected double nodeDistanceSquared(final double x, final double y, final double z, final Node node) {
        final double dx = x - (node.x() + 0.5D);
        final double dy = y - (node.y() + 0.5D);
        final double dz = z - (node.z() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Retrieves a set of node identifiers within a specified radius from a given location.
     * The method filters nodes to include only those that exist in the same world as
     * the provided location and lie within the specified squared radius distance.
     *
     * @param location the {@code Location} representing the center of the radius search
     * @param radiusSq the squared radius within which nodes will be searched
     * @return a {@code Set} of {@link UUID}s representing the graph IDs of the nodes within the specified radius
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
     * Calculates the squared spawn radius based on the given view distance and buffer.
     * The squared value is computed to avoid the overhead of calculating the square root
     * when the squared radius is sufficient for determining distances.
     *
     * @param viewDistance the distance at which elements should be visible
     * @param buffer       an additional buffer distance to be added to the view distance
     * @return the squared spawn radius calculated as (viewDistance + buffer)²
     */
    protected double spawnRadiusSquared(final double viewDistance, final double buffer) {
        final double r = viewDistance + buffer;
        return r * r;
    }
}
