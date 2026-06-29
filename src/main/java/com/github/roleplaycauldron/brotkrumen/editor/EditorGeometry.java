package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import org.bukkit.Location;

import java.util.Optional;

/**
 * Geometry helpers used by editor selection, placement, and teleport targets.
 */
final class EditorGeometry {

    private EditorGeometry() {
    }

    /* default */
    static boolean sameWorld(final Node node, final Location loc) {
        return loc.getWorld() != null && loc.getWorld().getUID().equals(node.worldId());
    }

    /* default */
    static double distance(final Node node, final Location loc) {
        if (!sameWorld(node, loc)) {
            return Double.MAX_VALUE;
        }
        final double deltaX = loc.getX() - node.x();
        final double deltaY = loc.getY() - node.y();
        final double deltaZ = loc.getZ() - node.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /* default */
    static double distance(final Node first, final Node second) {
        final double deltaX = first.x() - second.x();
        final double deltaY = first.y() - second.y();
        final double deltaZ = first.z() - second.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /* default */
    static double edgeDistance(final Graph graph, final Edge edge, final Location loc) {
        final Node source = graph.getNodeById(edge.source());
        final Node target = graph.getNodeById(edge.target());
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Double.MAX_VALUE;
        }
        return segmentDistance(source, target, loc);
    }

    /* default */
    static double segmentDistance(final Node source, final Node target, final Location loc) {
        final double directionX = target.x() - source.x();
        final double directionY = target.y() - source.y();
        final double directionZ = target.z() - source.z();
        final double lengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        final double offset = lengthSquared == 0.0D ? 0.0D
                : ((loc.getX() - source.x()) * directionX + (loc.getY() - source.y()) * directionY
                   + (loc.getZ() - source.z()) * directionZ) / lengthSquared;
        final double pathOffset = Math.max(0.0D, Math.min(1.0D, offset));
        final double nearestX = source.x() + directionX * pathOffset;
        final double nearestY = source.y() + directionY * pathOffset;
        final double nearestZ = source.z() + directionZ * pathOffset;
        final double deltaX = loc.getX() - nearestX;
        final double deltaY = loc.getY() - nearestY;
        final double deltaZ = loc.getZ() - nearestZ;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /* default */
    static Optional<Location> edgeMidpoint(final Graph graph, final Edge edge, final Location loc) {
        if (graph == null) {
            return Optional.empty();
        }
        final Node source = graph.getNodeById(edge.source());
        final Node target = graph.getNodeById(edge.target());
        return midpoint(source, target, loc);
    }

    /* default */
    static Optional<Location> midpoint(final Node source, final Node target, final Location loc) {
        if (source == null || target == null || !sameWorld(source, loc) || !sameWorld(target, loc)) {
            return Optional.empty();
        }
        return Optional.of(location(loc, (source.x() + target.x()) / 2.0D, (source.y() + target.y()) / 2.0D,
                (source.z() + target.z()) / 2.0D));
    }

    /* default */
    static Location location(final Location source, final double targetX, final double targetY, final double targetZ) {
        return new Location(source.getWorld(), targetX, targetY, targetZ, source.getYaw(), source.getPitch());
    }
}
