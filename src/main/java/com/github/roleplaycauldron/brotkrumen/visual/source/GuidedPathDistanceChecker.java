package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Distance checks for guided path route geometry.
 */
final class GuidedPathDistanceChecker {

    private static final double ZERO_DISTANCE = 0.0D;

    private GuidedPathDistanceChecker() {
    }

    /* default */
    static boolean viewerWithinRange(final Location location, final VisualGraphSnapshot snapshot,
                                     final List<NodeRef> visiblePath, final double distance) {
        final double normalizedDistance = Math.max(ZERO_DISTANCE, distance);
        final double distanceSquared = normalizedDistance * normalizedDistance;
        final Map<NodeRef, VisualNode> nodesByRef = snapshot.nodesByRef();
        return isNearAnyNode(location, visiblePath, nodesByRef, distanceSquared)
                || isNearAnyEdge(location, visiblePath, snapshot.edges(), nodesByRef, distanceSquared);
    }

    private static boolean isNearAnyNode(final Location location, final List<NodeRef> visiblePath,
                                         final Map<NodeRef, VisualNode> nodesByRef, final double distanceSquared) {
        for (final NodeRef ref : visiblePath) {
            final VisualNode node = nodesByRef.get(ref);
            if (node != null && isNodeWithinRange(location, node.node(), distanceSquared)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearAnyEdge(final Location location, final List<NodeRef> visiblePath,
                                         final Collection<VisualEdge> edges,
                                         final Map<NodeRef, VisualNode> nodesByRef,
                                         final double distanceSquared) {
        for (int i = 0; i + 1 < visiblePath.size(); i++) {
            final VisualNode source = nodesByRef.get(visiblePath.get(i));
            final VisualNode target = nodesByRef.get(visiblePath.get(i + 1));
            if (source != null && target != null
                    && hasVisiblePathEdge(edges, source.ref(), target.ref())
                    && isSegmentWithinRange(location, source.node(), target.node(), distanceSquared)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVisiblePathEdge(final Collection<VisualEdge> edges, final NodeRef source,
                                              final NodeRef target) {
        final List<NodeRef> segment = List.of(source, target);
        return edges.stream().anyMatch(edge -> PathEdgeMatcher.matchesPathWindow(edge, segment, 0, segment.size()));
    }

    private static boolean isNodeWithinRange(final Location location, final Node node, final double distanceSquared) {
        if (!sameWorld(location, node)) {
            return false;
        }
        final double deltaX = location.getX() - (node.x() + 0.5D);
        final double deltaY = location.getY() - (node.y() + 0.5D);
        final double deltaZ = location.getZ() - (node.z() + 0.5D);
        return ((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ)) <= distanceSquared;
    }

    private static boolean isSegmentWithinRange(final Location location, final Node start, final Node end,
                                                final double distanceSquared) {
        if (!sameWorld(location, start) || !sameWorld(location, end)) {
            return false;
        }
        final double startX = start.x() + 0.5D;
        final double startY = start.y() + 0.5D;
        final double startZ = start.z() + 0.5D;
        final double segmentX = end.x() + 0.5D - startX;
        final double segmentY = end.y() + 0.5D - startY;
        final double segmentZ = end.z() + 0.5D - startZ;
        final double segmentLengthSquared = (segmentX * segmentX) + (segmentY * segmentY) + (segmentZ * segmentZ);
        if (segmentLengthSquared <= ZERO_DISTANCE) {
            return isNodeWithinRange(location, start, distanceSquared);
        }
        return distanceToSegmentSquared(location, startX, startY, startZ, new SegmentVector(segmentX, segmentY, segmentZ,
                segmentLengthSquared)) <= distanceSquared;
    }

    private static double distanceToSegmentSquared(final Location location, final double startX, final double startY,
                                                   final double startZ, final SegmentVector segment) {
        final double viewerX = location.getX() - startX;
        final double viewerY = location.getY() - startY;
        final double viewerZ = location.getZ() - startZ;
        final double progress = Math.clamp(
                ((viewerX * segment.vectorX()) + (viewerY * segment.vectorY()) + (viewerZ * segment.vectorZ()))
                        / segment.lengthSquared(), ZERO_DISTANCE, 1.0D);
        final double closestX = startX + (segment.vectorX() * progress);
        final double closestY = startY + (segment.vectorY() * progress);
        final double closestZ = startZ + (segment.vectorZ() * progress);
        final double deltaX = location.getX() - closestX;
        final double deltaY = location.getY() - closestY;
        final double deltaZ = location.getZ() - closestZ;
        return (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ);
    }

    private static boolean sameWorld(final Location location, final Node node) {
        return location.getWorld() == null || node.worldId() == null || location.getWorld().getUID().equals(node.worldId());
    }

    private record SegmentVector(double vectorX, double vectorY, double vectorZ, double lengthSquared) {
    }
}
