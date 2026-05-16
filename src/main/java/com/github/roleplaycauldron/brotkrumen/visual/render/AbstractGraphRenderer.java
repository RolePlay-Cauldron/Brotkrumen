package com.github.roleplaycauldron.brotkrumen.visual.render;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.design.EdgeDesign;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.design.NodeDesign;
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
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CommentRequired", "PMD.ShortVariable"})
public abstract class AbstractGraphRenderer<N, E> implements GraphRenderer {

    private static final double VIEW_DISTANCE = 16.0D;

    private static final double SPAWN_DISTANCE_BUFFER = 16.0D;

    protected final Brotkrumen plugin;

    protected final UUID viewerId;

    private final Map<VisualNodeId, N> activeNodes = new HashMap<>();

    private final Map<VisualEdgeId, E> activeEdges = new HashMap<>();

    private VisualGraphSnapshot lastSnapshot;

    private GraphDesignResolver lastDesigns;

    protected AbstractGraphRenderer(final Brotkrumen plugin, final UUID viewerId) {
        this.plugin = plugin;
        this.viewerId = viewerId;
    }

    @Override
    public final void apply(final VisualGraphSnapshot snapshot, final GraphDesignResolver designs) {
        final Player player = plugin.getServer().getPlayer(viewerId);
        if (player == null) {
            return;
        }

        this.lastSnapshot = snapshot;
        this.lastDesigns = designs;
        final Set<VisualNodeId> visibleNodeIds = visibleNodeIds(snapshot, player.getLocation());
        final Set<VisualEdgeId> visibleEdgeIds = visibleEdgeIds(snapshot, visibleNodeIds);

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
            if (visibleNodeIds.contains(node.id())) {
                activeNodes.compute(node.id(), (id, handle) -> updateNode(handle, node, designs.resolveNode(node), player));
            }
        }
        for (final VisualEdge edge : snapshot.edges()) {
            if (visibleEdgeIds.contains(edge.id())) {
                activeEdges.compute(edge.id(), (id, handle) -> updateEdge(handle, edge, snapshot, designs.resolveEdge(edge), player));
            }
        }
    }

    @Override
    public final void applyVisibilityOnly() {
        if (lastSnapshot != null) {
            apply(lastSnapshot, lastDesigns);
        }
    }

    @Override
    public final void shutdown() {
        activeNodes.values().forEach(this::removeNode);
        activeEdges.values().forEach(this::removeEdge);
        activeNodes.clear();
        activeEdges.clear();
    }

    protected abstract N updateNode(N handle, VisualNode node, NodeDesign design, Player player);

    protected abstract E updateEdge(E handle, VisualEdge edge, VisualGraphSnapshot snapshot, EdgeDesign design, Player player);

    protected abstract void removeNode(N handle);

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
                result.add(node.id());
            }
        }
        return result;
    }

    private Set<VisualEdgeId> visibleEdgeIds(final VisualGraphSnapshot snapshot, final Set<VisualNodeId> visibleNodeIds) {
        final Set<NodeRef> visibleRefs = new HashSet<>();
        for (final VisualNode node : snapshot.nodes()) {
            if (visibleNodeIds.contains(node.id())) {
                visibleRefs.add(node.ref());
            }
        }

        final Set<VisualEdgeId> result = new HashSet<>();
        for (final VisualEdge edge : snapshot.edges()) {
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
        final double radius = VIEW_DISTANCE + SPAWN_DISTANCE_BUFFER;
        return radius * radius;
    }
}
