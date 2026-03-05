package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockDisplayVisualiser extends GraphVisualiser {

    private static final float DISPLAY_SCALE = 0.4f;

    private static final float EDGE_THICKNESS = 0.15f;

    private static final double EDGE_NODE_CLEARANCE = 0.35D;

    private static final double MIN_EDGE_LENGTH = 1.0D;

    private static final float DISPLAY_HALF_EXTENT = DISPLAY_SCALE / 2.0f;

    private static final double VIEW_DISTANCE = 16.0D;

    private static final double SPAWN_DISTANCE_BUFFER = 16.0D;

    private final WrappedLogger log;

    private final Brotkrumen plugin;

    private final Map<UUID, Node> nodesById = new HashMap<>();

    private final Map<UUID, BlockDisplay> nodeDisplays = new HashMap<>();

    private final Map<UUID, BlockDisplay> edgeDisplays = new HashMap<>();

    public BlockDisplayVisualiser(final Brotkrumen plugin, final LoggerFactory loggerFactory,
                                  final Collection<Node> nodes, final Collection<Edge> edges) {
        super(loggerFactory, nodes, edges);
        this.log = loggerFactory.create(BlockDisplayVisualiser.class);
        this.plugin = plugin;

        for (final Node node : nodes) {
            nodesById.put(node.graphId(), node);
        }

        if (edges.isEmpty()) {
            log.info("No edges configured for BlockDisplayVisualiser graph.");
        }
    }

    @Override
    public void showFor(final Player player) {
        if (viewers.contains(player.getUniqueId())) {
            log.debugF("Player '%s' is already viewing graph", player.getName());
            return;
        }
        viewers.add(player.getUniqueId());
    }

    @Override
    public void hideFor(final Player player) {
        viewers.remove(player.getUniqueId());
        if (viewers.isEmpty()) {
            despawnAll();
            return;
        }

        for (final BlockDisplay display : nodeDisplays.values()) {
            if (!display.isValid()) {
                continue;
            }
            player.hideEntity(plugin, display);
        }
        for (final BlockDisplay display : edgeDisplays.values()) {
            if (!display.isValid()) {
                continue;
            }
            player.hideEntity(plugin, display);
        }
    }

    @Override
    public void shutdown() {
        despawnAll();
        viewers.clear();
    }

    @Override
    public boolean isViewer(final Player player) {
        return viewers.contains(player.getUniqueId());
    }

    @Override
    void visibilityUpdate() {
        for (final UUID uuid : new HashSet<>(viewers)) {
            final Player player = plugin.getServer().getPlayer(uuid);
            if (player == null) {
                viewers.remove(uuid);
                continue;
            }
            updateSpawnedDisplays(player);
            updateVisibilityFor(player);
        }
    }

    private void updateSpawnedDisplays(final Player player) {
        final Map<UUID, Node> nodesToSpawn = new HashMap<>();
        final double spawnRadiusSq = spawnRadiusSquared(VIEW_DISTANCE, SPAWN_DISTANCE_BUFFER);

        final Location loc = player.getLocation();
        final Set<UUID> nearby = nodesWithin(nodes, loc, spawnRadiusSq);
        for (final UUID nodeId : nearby) {
            final Node node = nodesById.get(nodeId);
            if (node != null) {
                nodesToSpawn.put(nodeId, node);
            }
        }

        syncNodeDisplays(nodesToSpawn);
        syncEdgeDisplays(nodesToSpawn.keySet());
    }

    private void syncNodeDisplays(final Map<UUID, Node> nodesToSpawn) {
        final Set<UUID> toRemove = new HashSet<>(nodeDisplays.keySet());
        toRemove.removeAll(nodesToSpawn.keySet());
        for (final UUID nodeId : toRemove) {
            final BlockDisplay display = nodeDisplays.remove(nodeId);
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        for (final Node node : nodesToSpawn.values()) {
            if (nodeDisplays.containsKey(node.graphId()) && nodeDisplays.get(node.graphId()).isValid()) {
                continue;
            }
            spawnNodeDisplay(node);
        }
    }

    private void spawnNodeDisplay(final Node node) {
        final Location loc = node.toCenterLocation();
        if (loc.getWorld() == null) {
            log.warn("Node is in an unloaded or invalid world: " + node.worldId());
            return;
        }

        final String markerTag = nodeDisplayTag(node.graphId());
        final BlockDisplay existingDisplay = findDisplayAt(loc, markerTag);
        if (existingDisplay != null) {
            nodeDisplays.put(node.graphId(), existingDisplay);
            return;
        }

        final BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class, entity -> {
            entity.setBlock(Material.COAL_BLOCK.createBlockData());
            entity.setPersistent(false);
            entity.addScoreboardTag(markerTag);
            entity.setTransformation(new Transformation(
                    new Vector3f(-DISPLAY_HALF_EXTENT, -DISPLAY_HALF_EXTENT, -DISPLAY_HALF_EXTENT),
                    new Quaternionf(),
                    new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                    new Quaternionf()
            ));
        });
        nodeDisplays.put(node.graphId(), display);
    }

    private void syncEdgeDisplays(final Set<UUID> activeNodeIds) {
        final Set<UUID> edgesToSpawn = new HashSet<>();
        for (final Edge edge : edges) {
            if (activeNodeIds.contains(edge.source()) && activeNodeIds.contains(edge.target())) {
                edgesToSpawn.add(edge.edgeId());
            }
        }

        final Set<UUID> toRemove = new HashSet<>(edgeDisplays.keySet());
        toRemove.removeAll(edgesToSpawn);
        for (final UUID edgeId : toRemove) {
            final BlockDisplay display = edgeDisplays.remove(edgeId);
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        for (final Edge edge : edges) {
            if (!edgesToSpawn.contains(edge.edgeId())) {
                continue;
            }
            if (edgeDisplays.containsKey(edge.edgeId()) && edgeDisplays.get(edge.edgeId()).isValid()) {
                continue;
            }
            spawnEdgeDisplay(edge);
        }
    }

    private void spawnEdgeDisplay(final Edge edge) {
        final Node source = nodesById.get(edge.source());
        final Node target = nodesById.get(edge.target());

        if (source == null || target == null) {
            log.warn("Edge references unknown node: " + edge.edgeId());
            return;
        }

        if (!source.worldId().equals(target.worldId())) {
            log.warn("Edge spans worlds and cannot be visualised: " + edge.edgeId());
            return;
        }

        final World world = Bukkit.getWorld(source.worldId());
        if (world == null) {
            log.warn("Edge world is unloaded or invalid: " + edge.edgeId());
            return;
        }

        final Vector3f sourceCenter = new Vector3f(source.x() + 0.5f, source.y() + 0.5f, source.z() + 0.5f);
        final Vector3f targetCenter = new Vector3f(target.x() + 0.5f, target.y() + 0.5f, target.z() + 0.5f);
        final Vector3f direction = new Vector3f(targetCenter).sub(sourceCenter);
        final double distance = direction.length();

        if (distance == 0.0D) {
            return;
        }

        final double maxLength = distance - (2.0D * EDGE_NODE_CLEARANCE);
        if (maxLength <= 0.0D) {
            return;
        }

        final double displayLength = Math.max(MIN_EDGE_LENGTH, Math.min(maxLength, distance));
        final double startOffset = (distance - displayLength) / 2.0D;
        direction.normalize();

        final Vector3f displayStart = new Vector3f(sourceCenter)
                .add(new Vector3f(direction).mul((float) startOffset));

        final Location displayLoc = new Location(
                world,
                displayStart.x,
                displayStart.y,
                displayStart.z
        );

        final String markerTag = edgeDisplayTag(edge.edgeId());
        final BlockDisplay existingDisplay = findDisplayAt(displayLoc, markerTag);
        if (existingDisplay != null) {
            edgeDisplays.put(edge.edgeId(), existingDisplay);
            return;
        }

        final float renderedLength = (float) displayLength;
        final Quaternionf rotation = new Quaternionf().rotateTo(0.0f, 0.0f, 1.0f, direction.x, direction.y, direction.z);
        final BlockDisplay edgeDisplay = world.spawn(displayLoc, BlockDisplay.class, entity -> {
            entity.setBlock(Material.WHITE_STAINED_GLASS_PANE.createBlockData());
            entity.setPersistent(false);
            entity.addScoreboardTag(markerTag);
            entity.setTransformation(new Transformation(
                    new Vector3f(-EDGE_THICKNESS / 2.0f, -EDGE_THICKNESS / 2.0f, 0.0f),
                    rotation,
                    new Vector3f(EDGE_THICKNESS, EDGE_THICKNESS, renderedLength),
                    new Quaternionf()
            ));
        });
        edgeDisplays.put(edge.edgeId(), edgeDisplay);
    }

    private void updateVisibilityFor(final Player player) {
        final Set<UUID> visibleNodes = visibleNodesFor(player);

        for (final Map.Entry<UUID, BlockDisplay> e : nodeDisplays.entrySet()) {
            final UUID nodeId = e.getKey();
            final BlockDisplay display = e.getValue();
            if (!display.isValid()) {
                continue;
            }

            if (visibleNodes.contains(nodeId)) {
                player.showEntity(plugin, display);
            } else {
                player.hideEntity(plugin, display);
            }
        }

        for (final Edge edge : edges) {
            final BlockDisplay display = edgeDisplays.get(edge.edgeId());
            if (display == null || !display.isValid()) continue;

            final boolean show = visibleNodes.contains(edge.source()) && visibleNodes.contains(edge.target());
            if (show) {
                player.showEntity(plugin, display);
            } else {
                player.hideEntity(plugin, display);
            }
        }
    }

    private Set<UUID> visibleNodesFor(final Player player) {
        final Location loc = player.getLocation();
        return nodesWithin(nodes, loc, radiusSquared(VIEW_DISTANCE));
    }

    private BlockDisplay findDisplayAt(final Location location, final String markerTag) {
        if (location.getWorld() == null) {
            return null;
        }

        final Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(
                location,
                0.25D,
                0.25D,
                0.25D,
                entity -> entity instanceof BlockDisplay && entity.getScoreboardTags().contains(markerTag)
        );
        for (final Entity entity : nearbyEntities) {
            if (entity instanceof final BlockDisplay blockDisplay && blockDisplay.isValid()) {
                return blockDisplay;
            }
        }
        return null;
    }

    private String nodeDisplayTag(final UUID nodeId) {
        return "brotkrumen_node_" + nodeId;
    }

    private String edgeDisplayTag(final UUID edgeId) {
        return "brotkrumen_edge_" + edgeId;
    }

    private void despawnAll() {
        if (nodeDisplays.isEmpty() && edgeDisplays.isEmpty()) {
            return;
        }

        for (final BlockDisplay display : nodeDisplays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        for (final BlockDisplay display : edgeDisplays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }

        nodeDisplays.clear();
        edgeDisplays.clear();
    }
}
