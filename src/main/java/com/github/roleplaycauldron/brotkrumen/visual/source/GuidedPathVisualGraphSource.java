package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Visual source that exposes a moving path window for one viewer.
 */
public class GuidedPathVisualGraphSource implements VisualGraphSource {

    private final VisualGraphSource delegate;

    private final List<NodeRef> path;

    private final Map<NodeRef, VisualNodeRole> pathRoles;

    private final ViewerLocationSource locationSource;

    private final GuidedPathOptions options;

    private int progressIndex;

    /**
     * Creates a guided path source from a structured path result.
     *
     * @param delegate       source to filter
     * @param result         structured path result
     * @param locationSource viewer location source
     * @param options        guided path options
     */
    public GuidedPathVisualGraphSource(final VisualGraphSource delegate, final PathResult result,
                                       final ViewerLocationSource locationSource, final GuidedPathOptions options) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        final PathResult safeResult = result == null ? PathResult.empty() : result;
        this.path = safeResult.nodes();
        this.pathRoles = PathVisualRoles.fromSegments(safeResult.segments());
        this.locationSource = locationSource;
        this.options = options == null ? GuidedPathOptions.defaults() : options;
        this.progressIndex = 0;
    }

    @Override
    public VisualGraphSnapshot snapshot() {
        final VisualGraphSnapshot snapshot = delegate.snapshot();
        advanceProgress(snapshot);

        final List<NodeRef> visiblePath = visiblePath();
        final Set<NodeRef> visiblePathRefs = new HashSet<>(visiblePath);
        final Map<NodeRef, VisualNode> nodesByRef = snapshot.nodesByRef();
        final List<VisualNode> nodes = visiblePath.stream()
                .map(nodesByRef::get)
                .filter(Objects::nonNull)
                .map(this::pathNode)
                .toList();
        final List<VisualEdge> edges = snapshot.edges().stream()
                .filter(edge -> visiblePathRefs.contains(edge.source()) && visiblePathRefs.contains(edge.target()))
                .filter(this::isVisiblePathEdge)
                .toList();
        return new VisualGraphSnapshot(nodes, edges, versionValue());
    }

    private VisualNode pathNode(final VisualNode node) {
        return new VisualNode(node.visualNodeId(), node.ref(), node.node(), pathRoles.getOrDefault(node.ref(), VisualNodeRole.DEFAULT));
    }

    @Override
    public long version() {
        advanceProgress(delegate.snapshot());
        return versionValue();
    }

    private void advanceProgress(final VisualGraphSnapshot snapshot) {
        if (path.isEmpty() || locationSource == null) {
            return;
        }

        final Location location = locationSource.location();
        if (location == null) {
            return;
        }

        final Map<NodeRef, VisualNode> nodes = snapshot.nodesByRef();
        final int endExclusive = Math.min(path.size(), progressIndex + options.windowSize());
        for (int i = endExclusive - 1; i > progressIndex; i--) {
            final VisualNode node = nodes.get(path.get(i));
            if (node != null && isReached(location, node.node())) {
                progressIndex = i;
                return;
            }
        }
    }

    private boolean isReached(final Location location, final Node node) {
        if (location.getWorld() != null && node.worldId() != null && !location.getWorld().getUID().equals(node.worldId())) {
            return false;
        }

        final double deltaX = location.getX() - (node.x() + 0.5D);
        final double deltaY = location.getY() - (node.y() + 0.5D);
        final double deltaZ = location.getZ() - (node.z() + 0.5D);
        return ((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ)) <= activationRadiusSquared();
    }

    private List<NodeRef> visiblePath() {
        final int start = Math.max(0, progressIndex - options.lookBehind());
        final int endExclusive = Math.min(path.size(), progressIndex + options.windowSize());
        return path.subList(start, endExclusive);
    }

    private boolean isVisiblePathEdge(final VisualEdge edge) {
        final int start = Math.max(0, progressIndex - options.lookBehind());
        final int endExclusive = Math.min(path.size(), progressIndex + options.windowSize());
        for (int i = start; i + 1 < endExclusive; i++) {
            if (edge.source().equals(path.get(i)) && edge.target().equals(path.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private double activationRadiusSquared() {
        return options.activationRadius() * options.activationRadius();
    }

    private long versionValue() {
        long result = delegate.version();
        result = (31 * result) + path.hashCode();
        result = (31 * result) + options.hashCode();
        result = (31 * result) + progressIndex;
        return result;
    }
}
