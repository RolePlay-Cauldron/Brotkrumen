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
     * Represents the squared radius around a viewer in which nodes and edges
     * of a visual graph are visibly rendered.
     */
    private final double configuredVisibleRadiusSquared;

    /**
     * Represents the squared radius around a viewer in which nodes and edges
     * of a visual graph are kept spawned or active.
     */
    private final double configuredSpawnRadiusSquared;

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
        this.configuredVisibleRadiusSquared = computeVisibleRadiusSquared(plugin);
        this.configuredSpawnRadiusSquared = computeSpawnRadiusSquared(plugin);
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
        final Location location = player.getLocation();
        final Set<VisualNodeId> retainedNodeIds = nodeIdsWithin(snapshot, location, activeRadiusSquared());
        final Set<VisualNodeId> visibleNodeIds = nodeIdsWithin(snapshot, location, visibleRadiusSquared());
        final Set<VisualEdgeId> retainedEdgeIds = edgeIdsFor(snapshot, retainedNodeIds, designs);
        final Set<VisualEdgeId> visibleEdgeIds = edgeIdsFor(snapshot, visibleNodeIds, designs);

        activeNodes.entrySet().removeIf(entry -> {
            if (!retainedNodeIds.contains(entry.getKey())) {
                removeNode(entry.getValue());
                return true;
            }
            return false;
        });
        activeEdges.entrySet().removeIf(entry -> {
            if (!retainedEdgeIds.contains(entry.getKey())) {
                removeEdge(entry.getValue());
                return true;
            }
            return false;
        });

        for (final VisualNode node : snapshot.nodes()) {
            if (retainedNodeIds.contains(node.visualNodeId())) {
                final N handle = activeNodes.compute(node.visualNodeId(), (id, existing) -> existing == null || updateExisting
                        ? updateNode(existing, node, designs, player)
                        : existing);
                updateNodeVisibility(handle, player, visibleNodeIds.contains(node.visualNodeId()));
            }
        }
        for (final VisualEdge edge : snapshot.edges()) {
            if (retainedEdgeIds.contains(edge.id())) {
                final E handle = activeEdges.compute(edge.id(), (id, existing) -> existing == null || updateExisting
                        ? updateEdge(existing, edge, snapshot, designs, player)
                        : existing);
                updateEdgeVisibility(handle, player, visibleEdgeIds.contains(edge.id()));
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
     * Applies viewer-specific visibility to a retained node handle.
     *
     * @param handle  renderer-specific node handle
     * @param player  viewer whose visibility is being updated
     * @param visible whether the handle should be visible to the viewer
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void updateNodeVisibility(final N handle, final Player player, final boolean visible) {
        // Most renderer handles are only retained while visible.
    }

    /**
     * Applies viewer-specific visibility to a retained edge handle.
     *
     * @param handle  renderer-specific edge handle
     * @param player  viewer whose visibility is being updated
     * @param visible whether the handle should be visible to the viewer
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void updateEdgeVisibility(final E handle, final Player player, final boolean visible) {
        // Most renderer handles are only retained while visible.
    }

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

    /**
     * Whether this renderer keeps handles active outside the visible radius.
     *
     * @return true when invisible handles should be retained within the configured spawn radius
     */
    protected boolean retainsInvisibleHandles() {
        return false;
    }

    private Set<VisualNodeId> nodeIdsWithin(final VisualGraphSnapshot snapshot, final Location location,
                                            final double radiusSquared) {
        final Set<VisualNodeId> result = new HashSet<>();
        final UUID worldId = location.getWorld().getUID();
        for (final VisualNode node : snapshot.nodes()) {
            if (!worldId.equals(node.node().worldId())) {
                continue;
            }
            if (nodeDistanceSquared(location, node) <= radiusSquared) {
                result.add(node.visualNodeId());
            }
        }
        return result;
    }

    private Set<VisualEdgeId> edgeIdsFor(final VisualGraphSnapshot snapshot, final Set<VisualNodeId> nodeIds,
                                         final GraphDesignResolver designs) {
        final Set<NodeRef> visibleRefs = new HashSet<>();
        for (final VisualNode node : snapshot.nodes()) {
            if (nodeIds.contains(node.visualNodeId())) {
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

    private double visibleRadiusSquared() {
        return configuredVisibleRadiusSquared;
    }

    private double activeRadiusSquared() {
        if (retainsInvisibleHandles()) {
            return spawnRadiusSquared();
        }
        return visibleRadiusSquared();
    }

    private double spawnRadiusSquared() {
        return configuredSpawnRadiusSquared;
    }

    private double computeVisibleRadiusSquared(final Brotkrumen plugin) {
        final double radius = configDistance(plugin, VIEW_DISTANCE_CONFIG, DEFAULT_VIEW_DISTANCE);
        return radius * radius;
    }

    private double computeSpawnRadiusSquared(final Brotkrumen plugin) {
        final double radius = configDistance(plugin, VIEW_DISTANCE_CONFIG, DEFAULT_VIEW_DISTANCE)
                + configDistance(plugin, SPAWN_DISTANCE_BUFFER_CONFIG, DEFAULT_SPAWN_DISTANCE_BUFFER);
        return radius * radius;
    }

    private double configDistance(final Brotkrumen plugin, final String path, final double defaultValue) {
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
