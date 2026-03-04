package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
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
}
