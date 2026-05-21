package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared snapshot reconciliation for graph renderers.
 *
 * @param <N> node handle type
 * @param <E> edge handle type
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ShortVariable", "PMD.CognitiveComplexity"})
public abstract class AbstractGraphRenderer<N, E> implements GraphRenderer {

    /**
     * Configuration path for the base distance used to decide visible graph elements.
     */
    private static final String VIEW_DISTANCE_CONFIG = "visualizer.viewDistance";

    /**
     * Configuration path for the additional distance that keeps rendered graph elements active.
     */
    private static final String SPAWN_DISTANCE_BUFFER_CONFIG = "visualizer.spawnDistanceBuffer";

    /**
     * Default base view distance used when no configuration value is available.
     */
    private static final double DEFAULT_VIEW_DISTANCE = 16.0D;

    /**
     * Default spawn distance buffer used when no configuration value is available.
     */
    private static final double DEFAULT_SPAWN_DISTANCE_BUFFER = 16.0D;

    /**
     * Minimum accepted configured distance.
     */
    private static final double MINIMUM_DISTANCE = 0.0D;

    /**
     * Plugin instance used to access Bukkit state and configuration.
     */
    protected final Brotkrumen plugin;

    /**
     * Unique id of the viewer this renderer reconciles for.
     */
    protected final UUID viewerId;

    /**
     * Currently active rendered node handles keyed by stable visual node id.
     */
    private final Map<VisualNodeId, N> activeNodes = new HashMap<>();

    /**
     * Currently active rendered edge handles keyed by stable visual edge id.
     */
    private final Map<VisualEdgeId, E> activeEdges = new HashMap<>();

    /**
     * Last applied snapshot used when only viewer visibility needs to be updated.
     */
    private VisualGraphSnapshot lastSnapshot;

    /**
     * Last design resolver used with the cached snapshot.
     */
    private GraphDesignResolver lastDesigns;

    /**
     * Creates a renderer for one viewer.
     *
     * @param plugin   plugin used to access server state and configuration
     * @param viewerId viewer id
     */
    protected AbstractGraphRenderer(final Brotkrumen plugin, final UUID viewerId) {
        this.plugin = plugin;
        this.viewerId = viewerId;
    }

    @Override
    public final void apply(final VisualGraphSnapshot snapshot, final GraphDesignResolver designs) {
        reconcile(snapshot, designs, true);
    }

    @Override
    public final void applyVisibilityOnly() {
        if (lastSnapshot != null) {
            reconcile(lastSnapshot, lastDesigns, false);
        }
    }

    @Override
    public final void shutdown() {
        activeNodes.values().forEach(this::removeNode);
        activeEdges.values().forEach(this::removeEdge);
        activeNodes.clear();
        activeEdges.clear();
    }

    private void reconcile(final VisualGraphSnapshot snapshot, final GraphDesignResolver designs,
                           final boolean updateExisting) {
        final Player player = plugin.getServer().getPlayer(viewerId);
        if (player == null) {
            return;
        }

        this.lastSnapshot = snapshot;
        this.lastDesigns = designs;
        final Set<VisualNodeId> visibleNodeIds = visibleNodeIds(snapshot, player.getLocation());
        final Set<VisualEdgeId> visibleEdgeIds = visibleEdgeIds(snapshot, visibleNodeIds, designs);

        activeNodes.entrySet().removeIf(entry -> {
            if (!visibleNodeIds.contains(entry.getKey())) {
                removeNode(entry.getValue());
                return true;
            }
            return false;
        });
        activeEdges.entrySet().removeIf(entry -> {
            if (!visibleEdgeIds.contains(entry.getKey())) {
                removeEdge(entry.getValue());
                return true;
            }
            return false;
        });

        for (final VisualNode node : snapshot.nodes()) {
            if (visibleNodeIds.contains(node.visualNodeId())) {
                activeNodes.compute(node.visualNodeId(), (id, handle) -> handle == null || updateExisting
                        ? updateNode(handle, node, designs, player)
                        : handle);
            }
        }
        for (final VisualEdge edge : snapshot.edges()) {
            if (visibleEdgeIds.contains(edge.id())) {
                activeEdges.compute(edge.id(), (id, handle) -> handle == null || updateExisting
                        ? updateEdge(handle, edge, snapshot, designs, player)
                        : handle);
            }
        }
    }

    /**
     * Creates or updates the rendered representation of a visible node.
     *
     * @param handle  existing renderer-specific node handle, or {@code null} when none exists yet
     * @param node    visual node to render
     * @param designs resolver for renderer-specific designs
     * @param player  viewer that should see the rendered node
     * @return active node handle to keep for future reconciliation
     */
    protected abstract N updateNode(N handle, VisualNode node, GraphDesignResolver designs, Player player);

    /**
     * Creates or updates the rendered representation of a visible edge.
     *
     * @param handle   existing renderer-specific edge handle, or {@code null} when none exists yet
     * @param edge     visual edge to render
     * @param snapshot source snapshot containing endpoint nodes
     * @param designs  resolver for renderer-specific designs
     * @param player   viewer that should see the rendered edge
     * @return active edge handle to keep for future reconciliation
     */
    protected abstract E updateEdge(E handle, VisualEdge edge, VisualGraphSnapshot snapshot,
                                    GraphDesignResolver designs, Player player);

    /**
     * Removes a previously active node handle from the renderer.
     *
     * @param handle renderer-specific node handle
     */
    protected abstract void removeNode(N handle);

    /**
     * Removes a previously active edge handle from the renderer.
     *
     * @param handle renderer-specific edge handle
     */
    protected abstract void removeEdge(E handle);

    private Set<VisualNodeId> visibleNodeIds(final VisualGraphSnapshot snapshot, final Location location) {
        final Set<VisualNodeId> result = new HashSet<>();
        final double radiusSq = spawnRadiusSquared();
        final UUID worldId = location.getWorld().getUID();
        for (final VisualNode node : snapshot.nodes()) {
            if (!worldId.equals(node.node().worldId())) {
                continue;
            }
            if (nodeDistanceSquared(location, node) <= radiusSq) {
                result.add(node.visualNodeId());
            }
        }
        return result;
    }

    private Set<VisualEdgeId> visibleEdgeIds(final VisualGraphSnapshot snapshot, final Set<VisualNodeId> visibleNodeIds,
                                             final GraphDesignResolver designs) {
        final Set<NodeRef> visibleRefs = new HashSet<>();
        for (final VisualNode node : snapshot.nodes()) {
            if (visibleNodeIds.contains(node.visualNodeId())) {
                visibleRefs.add(node.ref());
            }
        }

        final Set<VisualEdgeId> result = new HashSet<>();
        for (final VisualEdge edge : snapshot.edges()) {
            if (designs.resolveEdgeRenderStrategy(edge) == EdgeRenderStrategy.ENDPOINTS_ONLY) {
                continue;
            }
            if (visibleRefs.contains(edge.source()) && visibleRefs.contains(edge.target())) {
                result.add(edge.id());
            }
        }
        return result;
    }

    private double nodeDistanceSquared(final Location location, final VisualNode node) {
        final double dx = location.getX() - (node.node().x() + 0.5D);
        final double dy = location.getY() - (node.node().y() + 0.5D);
        final double dz = location.getZ() - (node.node().z() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private double spawnRadiusSquared() {
        final double radius = configDistance(VIEW_DISTANCE_CONFIG, DEFAULT_VIEW_DISTANCE)
                + configDistance(SPAWN_DISTANCE_BUFFER_CONFIG, DEFAULT_SPAWN_DISTANCE_BUFFER);
        return radius * radius;
    }

    private double configDistance(final String path, final double defaultValue) {
        if (plugin == null) {
            return defaultValue;
        }
        final double configured = plugin.getConfig().getDouble(path, defaultValue);
        if (configured < MINIMUM_DISTANCE) {
            return defaultValue;
        }
        return configured;
    }
}
