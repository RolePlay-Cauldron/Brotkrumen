package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class GraphVisualiser {

    protected final LoggerFactory loggerFactory;

    protected final Collection<Node> nodes;

    protected final Collection<Edge> edges;

    protected final Set<UUID> viewers;

    public GraphVisualiser(final LoggerFactory loggerFactory, final Collection<Node> nodes, final Collection<Edge> edges) {
        this.loggerFactory = loggerFactory;
        this.nodes = nodes;
        this.edges = edges;
        this.viewers = new HashSet<>();
    }

    public abstract void showFor(Player player);

    public abstract void hideFor(Player player);

    public abstract void shutdown();

    public abstract boolean isViewer(Player player);

    abstract void visibilityUpdate();

    public double nodeDistanceSquared(final double x, final double y, final double z, final Node node) {
        final double dx = x - (node.x() + 0.5D);
        final double dy = y - (node.y() + 0.5D);
        final double dz = z - (node.z() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    public Set<UUID> nodesWithin(final Collection<Node> nodes, final Location location, final double radiusSq) {
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

    public boolean anyNodeWithin(final Collection<Node> nodes, final World world,
                                 final double x, final double y, final double z, final double radiusSq) {
        final UUID wid = world.getUID();
        for (final Node node : nodes) {
            if (!node.worldId().equals(wid)) continue;
            if (nodeDistanceSquared(x, y, z, node) <= radiusSq) return true;
        }
        return false;
    }

    public double radiusSquared(final double radius) {
        return radius * radius;
    }

    public double spawnRadiusSquared(final double viewDistance, final double buffer) {
        final double r = viewDistance + buffer;
        return r * r;
    }
}
