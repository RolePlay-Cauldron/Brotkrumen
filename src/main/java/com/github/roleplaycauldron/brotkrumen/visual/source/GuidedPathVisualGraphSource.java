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
@SuppressWarnings("PMD.TooManyMethods")
public class GuidedPathVisualGraphSource implements VisualGraphSource {

    private final VisualGraphSource delegate;

    private final List<NodeRef> path;

    private final Map<NodeRef, VisualNodeRole> pathRoles;

    private final ViewerLocationSource locationSource;

    private final GuidedPathOptions options;

    private final boolean goalMarkerEnabled;

    private int progressIndex;

    private boolean completed;

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
        this(delegate, result, locationSource, options, false);
    }

    /**
     * Creates a guided path source from a structured path result.
     *
     * @param delegate          source to filter
     * @param result            structured path result
     * @param locationSource    viewer location source
     * @param options           guided path options
     * @param goalMarkerEnabled whether the final path node should use goal marker role
     */
    public GuidedPathVisualGraphSource(final VisualGraphSource delegate, final PathResult result,
                                       final ViewerLocationSource locationSource, final GuidedPathOptions options,
                                       final boolean goalMarkerEnabled) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        final PathResult safeResult = result == null ? PathResult.empty() : result;
        this.path = safeResult.nodes();
        this.pathRoles = PathVisualRoles.fromSegments(safeResult.segments());
        this.locationSource = locationSource;
        this.options = options == null ? GuidedPathOptions.defaults() : options;
        this.goalMarkerEnabled = goalMarkerEnabled;
        this.progressIndex = 0;
        this.completed = false;
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
        if (goalMarkerEnabled && !path.isEmpty() && node.ref().equals(path.getLast())) {
            return new VisualNode(node.visualNodeId(), node.ref(), node.node(), VisualNodeRole.GUIDED_PATH_GOAL);
        }
        return new VisualNode(node.visualNodeId(), node.ref(), node.node(),
                pathRoles.getOrDefault(node.ref(), VisualNodeRole.DEFAULT));
    }

    @Override
    public long version() {
        advanceProgress(delegate.snapshot());
        return versionValue();
    }

    private void advanceProgress(final VisualGraphSnapshot snapshot) {
        if (path.isEmpty() || locationSource == null) {
            completed = false;
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
                break;
            }
        }

        final VisualNode finalNode = snapshot.nodesByRef().get(path.getLast());
        if (finalNode != null && isReached(location, finalNode.node())) {
            progressIndex = path.size() - 1;
            completed = true;
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
        final int start = Math.max(0, progressIndex - effectiveLookBehind());
        final int endExclusive = Math.min(path.size(), progressIndex + options.windowSize());
        return path.subList(start, endExclusive);
    }

    private boolean isVisiblePathEdge(final VisualEdge edge) {
        final int start = Math.max(0, progressIndex - effectiveLookBehind());
        final int endExclusive = Math.min(path.size(), progressIndex + options.windowSize());
        return PathEdgeMatcher.matchesPathWindow(edge, path, start, endExclusive);
    }

    private int effectiveLookBehind() {
        if (completed && !options.keepLookBehindOnCompletion()) {
            return 0;
        }
        return options.lookBehind();
    }

    private double activationRadiusSquared() {
        return options.activationRadius() * options.activationRadius();
    }

    private long versionValue() {
        long result = delegate.version();
        result = (31 * result) + path.hashCode();
        result = (31 * result) + options.hashCode();
        result = (31 * result) + progressIndex;
        result = (31 * result) + Boolean.hashCode(completed);
        return result;
    }

    /**
     * Returns whether the final path node was reached.
     *
     * @return true when completed
     */
    public boolean complete() {
        return completed;
    }

    /**
     * Returns the current reached path node index.
     *
     * @return progress index
     */
    public int currentProgressIndex() {
        return progressIndex;
    }

    /**
     * Returns whether the viewer is within range of the current guided path window.
     *
     * @param distance maximum distance from a visible guided node or edge
     * @return true when the viewer is within range
     */
    public boolean viewerWithinCurrentPath(final double distance) {
        if (locationSource == null) {
            return true;
        }
        final Location location = locationSource.location();
        if (location == null) {
            return true;
        }
        final VisualGraphSnapshot snapshot = delegate.snapshot();
        advanceProgress(snapshot);
        return GuidedPathDistanceChecker.viewerWithinRange(location, snapshot, visiblePath(), distance);
    }
}
