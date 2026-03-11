package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CommentRequired"})
public class ParticleVisualizer extends GraphVisualizer {

    private static final double VIEW_DISTANCE = 16.0D;

    private static final double SPAWN_DISTANCE_BUFFER = 16.0D;

    private static final int PATH_NODE_WINDOW_SIZE = 4;

    private static final double PATH_NODE_ACTIVATION_RADIUS_SQ = 16.0D;

    private final Brotkrumen plugin;

    private final EffectManager effectManager;

    private final EffectLibConfig nodeConfig;

    private final EffectLibConfig edgeConfig;

    private final UUID ownerId;

    private final VisualMode mode;

    private final List<Node> path;

    private final Map<UUID, Map<UUID, Edge>> edgesByNodes = new HashMap<>();

    private final Map<UUID, Effect> activeNodeEffects = new HashMap<>();

    private final Map<UUID, Effect> activeEdgeEffects = new HashMap<>();

    private int pathNodeIndex;

    public ParticleVisualizer(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                              final Graph graph, final EffectManager effectManager,
                              final EffectLibConfig nodeConfig, final EffectLibConfig edgeConfig,
                              final UUID ownerId, final VisualMode mode, final List<Node> path) {
        super(loggerFactory, graph);
        this.plugin = plugin;
        this.effectManager = effectManager;
        this.nodeConfig = nodeConfig;
        this.edgeConfig = edgeConfig;
        this.ownerId = ownerId;
        this.mode = mode;
        this.path = path;
        this.pathNodeIndex = 0;

        for (final Edge edge : graph.getEdges()) {
            edgesByNodes.computeIfAbsent(edge.source(), k -> new HashMap<>()).put(edge.target(), edge);
            edgesByNodes.computeIfAbsent(edge.target(), k -> new HashMap<>()).put(edge.source(), edge);
        }
    }

    @Override
    public void shutdown() {
        clearEffects();
    }

    /* default */
    @Override
    void visibilityUpdate() {
        final Player player = plugin.getServer().getPlayer(ownerId);
        if (player == null) {
            return;
        }

        if (mode == VisualMode.PATH_FINDER) {
            updatePathFinderEffects(player);
        } else {
            updateEditEffects(player);
        }
    }

    private void updateEditEffects(final Player player) {
        final double spawnRadiusSq = spawnRadiusSquared(VIEW_DISTANCE, SPAWN_DISTANCE_BUFFER);
        final Location loc = player.getLocation();
        final Set<UUID> nearbyNodeIds = nodesWithin(loc, spawnRadiusSq);

        syncEffects(activeNodeEffects, nearbyNodeIds, nodeId -> {
            final Node node = graph.getNodeById(nodeId);
            return node != null ? startNodeEffect(node.toCenterLocation(), player) : null;
        });

        final Set<UUID> activeEdgeIds = new HashSet<>();
        for (final UUID nodeId : nearbyNodeIds) {
            final Map<UUID, Edge> connections = edgesByNodes.get(nodeId);
            if (connections == null) {
                continue;
            }
            for (final Edge edge : connections.values()) {
                if (nearbyNodeIds.contains(edge.source()) && nearbyNodeIds.contains(edge.target())) {
                    activeEdgeIds.add(edge.edgeId());
                }
            }
        }

        syncEffects(activeEdgeEffects, activeEdgeIds, edgeId -> {
            final Edge edge = graph.getEdgeById(edgeId);
            return edge != null ? startEdgeEffect(edge, player) : null;
        });
    }

    private void syncEffects(final Map<UUID, Effect> registry, final Set<UUID> targetIds,
                             final Function<UUID, Effect> spawner) {
        registry.entrySet().removeIf(entry -> {
            if (!targetIds.contains(entry.getKey())) {
                stopEffect(entry.getValue());
                return true;
            }
            return false;
        });

        for (final UUID id : targetIds) {
            Effect effect = registry.get(id);
            if (effect == null) {
                effect = spawner.apply(id);
                if (effect != null) {
                    registry.put(id, effect);
                }
            }
        }
    }

    private void updatePathFinderEffects(final Player player) {
        final List<Node> allNodes = path != null ? path : new ArrayList<>(graph.getNodes());
        if (allNodes.isEmpty()) {
            return;
        }

        final Location loc = player.getLocation();
        final double locX = loc.getX();
        final double locY = loc.getY();
        final double locZ = loc.getZ();

        final int startIdx = Math.max(0, pathNodeIndex - 1);
        final int endIdx = Math.min(allNodes.size() - 1, pathNodeIndex + PATH_NODE_WINDOW_SIZE - 1);

        for (int i = endIdx; i >= startIdx; i--) {
            if (nodeDistanceSquared(locX, locY, locZ, allNodes.get(i)) < PATH_NODE_ACTIVATION_RADIUS_SQ) {
                pathNodeIndex = i;
                break;
            }
        }

        final double spawnRadiusSq = spawnRadiusSquared(VIEW_DISTANCE, SPAWN_DISTANCE_BUFFER);
        final List<Node> displayNodes = new ArrayList<>();
        for (int i = pathNodeIndex; i < Math.min(allNodes.size(), pathNodeIndex + PATH_NODE_WINDOW_SIZE); i++) {
            final Node node = allNodes.get(i);
            if (nodeDistanceSquared(locX, locY, locZ, node) <= spawnRadiusSq) {
                displayNodes.add(node);
            }
        }

        final List<Edge> visibleEdges = edgesBetweenNeighbours(displayNodes);

        final Set<UUID> displayNodeIds = new HashSet<>();
        for (final Node node : displayNodes) {
            displayNodeIds.add(node.graphId());
        }
        syncEffects(activeNodeEffects, displayNodeIds, nodeId -> {
            final Node node = graph.getNodeById(nodeId);
            return node != null ? startNodeEffect(node.toCenterLocation(), player) : null;
        });

        final Set<UUID> visibleEdgeIds = new HashSet<>();
        for (final Edge edge : visibleEdges) {
            visibleEdgeIds.add(edge.edgeId());
        }
        syncEffects(activeEdgeEffects, visibleEdgeIds, edgeId -> {
            final Edge edge = graph.getEdgeById(edgeId);
            return edge != null ? startEdgeEffect(edge, player) : null;
        });
    }

    private List<Edge> edgesBetweenNeighbours(final List<Node> nodesInOrder) {
        final List<Edge> result = new ArrayList<>();
        for (int i = 0; i + 1 < nodesInOrder.size(); i++) {
            final Edge edge = findEdge(nodesInOrder.get(i).graphId(), nodesInOrder.get(i + 1).graphId());
            if (edge != null) {
                result.add(edge);
            }
        }
        return result;
    }

    private Edge findEdge(final UUID first, final UUID second) {
        final Map<UUID, Edge> connections = edgesByNodes.get(first);
        return connections != null ? connections.get(second) : null;
    }

    private Effect startNodeEffect(final Location location, final Player player) {
        return effectManager.start(nodeConfig.effectClass(), nodeConfig.settings(), location, player);
    }

    private Effect startEdgePlaceholderEffect(final Location location, final Player player) {
        return effectManager.start(edgeConfig.effectClass(), edgeConfig.settings(), location, player);
    }

    private Effect startEdgeEffect(final Edge edge, final Player player) {
        final Node source = graph.getNodeById(edge.source());
        if (source == null) {
            return null;
        }
        final Effect effect = startEdgePlaceholderEffect(source.toCenterLocation(), player);
        if (effect != null) {
            updateEdgeEffectLocation(effect, edge);
        }
        return effect;
    }

    private void updateEdgeEffectLocation(final Effect effect, final Edge edge) {
        final Node source = graph.getNodeById(edge.source());
        final Node target = graph.getNodeById(edge.target());
        if (source == null || target == null) {
            return;
        }

        effect.setLocation(source.toCenterLocation());
        if (effect instanceof final LineEffect lineEffect) {
            lineEffect.setTarget(target.toCenterLocation());
        }
    }

    private void clearEffects() {
        activeNodeEffects.values().forEach(this::stopEffect);
        activeEdgeEffects.values().forEach(this::stopEffect);

        activeNodeEffects.clear();
        activeEdgeEffects.clear();
    }

    private void stopEffect(final Effect effect) {
        if (effect != null) {
            effect.cancel();
        }
    }
}
