package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GraphSpatial {

    private GraphSpatial() {
    }

    public static double nodeDistanceSquared(final double x, final double y, final double z, final Node node) {
        final double dx = x - (node.x() + 0.5D);
        final double dy = y - (node.y() + 0.5D);
        final double dz = z - (node.z() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    public static Set<UUID> nodesWithin(final Collection<Node> nodes, final Location location, final double radiusSq) {
        final double xLoc = location.getX();
        final double yLoc = location.getY();
        final double zLoc = location.getZ();
        final World world = location.getWorld();

        final Set<UUID> out = new HashSet<>();
        final UUID wid = world.getUID();
        for (final Node node : nodes) {
            if (!node.worldId().equals(wid)) continue;
            if (nodeDistanceSquared(xLoc, yLoc, zLoc, node) <= radiusSq) {
                out.add(node.graphId());
            }
        }
        return out;
    }

    public static boolean anyNodeWithin(final Collection<Node> nodes, final World world,
                                        final double x, final double y, final double z, final double radiusSq) {
        final UUID wid = world.getUID();
        for (final Node node : nodes) {
            if (!node.worldId().equals(wid)) continue;
            if (nodeDistanceSquared(x, y, z, node) <= radiusSq) return true;
        }
        return false;
    }

    public static double radiusSquared(final double radius) {
        return radius * radius;
    }

    public static double spawnRadiusSquared(final double viewDistance, final double buffer) {
        final double r = viewDistance + buffer;
        return r * r;
    }
}
