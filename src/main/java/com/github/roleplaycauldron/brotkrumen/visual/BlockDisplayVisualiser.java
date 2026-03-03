package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
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

public class BlockDisplayVisualiser implements GraphVisualiser {

    private static final float DISPLAY_SCALE = 0.4f;

    private static final float EDGE_THICKNESS = 0.15f;

    private static final double EDGE_NODE_CLEARANCE = 0.35D;

    private static final double MIN_EDGE_LENGTH = 1.0D;

    private static final float DISPLAY_HALF_EXTENT = DISPLAY_SCALE / 2.0f;

    private static final double VIEW_DISTANCE = 48.0D;

    private static final double VIEW_DISTANCE_SQUARED = VIEW_DISTANCE * VIEW_DISTANCE;

    private static final double SPAWN_DISTANCE_BUFFER = 16.0D;

    private static final long VISIBILITY_CHECK_PERIOD_TICKS = 10L;

    private final Brotkrumen plugin;

    private final Collection<Node> nodes;

    private final Collection<Edge> edges;

    private final Map<UUID, Node> nodesById = new HashMap<>();

    private final Map<UUID, BlockDisplay> nodeDisplays = new HashMap<>();

    private final Map<UUID, BlockDisplay> edgeDisplays = new HashMap<>();

    private final Set<UUID> viewers = new HashSet<>();

    private int rotationTaskId = -1;

    private int visibilityTaskId = -1;

    public BlockDisplayVisualiser(final Brotkrumen plugin, final Collection<Node> nodes, final Collection<Edge> edges) {
        this.plugin = plugin;
        this.nodes = nodes;
        this.edges = edges;

        for (final Node node : nodes) {
            nodesById.put(node.graphId(), node);
        }

        if (edges.isEmpty()) {
            plugin.getLogger().warning("No edges configured for BlockDisplayVisualiser graph.");
        }

        startVisibilityTask();
        startRotationTask();
    }

    private double spawnDistanceSquared() {
        return VIEW_DISTANCE * SPAWN_DISTANCE_BUFFER;
    }

    private void updateSpawnedDisplays() {
        final Map<UUID, Node> nodesToSpawn = new HashMap<>();
        final double spawnDistanceSquared = spawnDistanceSquared();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final World playerWorld = player.getWorld();
            final double playerX = player.getLocation().getX();
            final double playerY = player.getLocation().getY();
            final double playerZ = player.getLocation().getZ();

            for (final Node node : nodes) {
                if (!node.worldId().equals(playerWorld.getUID())) {
                    continue;
                }

                final double dx = playerX - (node.x() + 0.5D);
                final double dy = playerY - (node.y() + 0.5D);
                final double dz = playerZ - (node.z() + 0.5D);
                final double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);
                if (distanceSquared <= spawnDistanceSquared) {
                    nodesToSpawn.put(node.graphId(), node);
                }
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
            plugin.getLogger().warning("Node is in an unloaded or invalid world: " + node.worldId());
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
            plugin.getLogger().warning("Edge references unknown node: " + edge.edgeId());
            return;
        }

        if (!source.worldId().equals(target.worldId())) {
            plugin.getLogger().warning("Edge spans worlds and cannot be visualised: " + edge.edgeId());
            return;
        }

        final World world = Bukkit.getWorld(source.worldId());
        if (world == null) {
            plugin.getLogger().warning("Edge world is unloaded or invalid: " + edge.edgeId());
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
            entity.setBlock(Material.GLASS_PANE.createBlockData());
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

    private void startVisibilityTask() {
        visibilityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::updateAllViewers,
                0L,
                VISIBILITY_CHECK_PERIOD_TICKS
        );
    }

    private void updateAllViewers() {
        updateSpawnedDisplays();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            updateViewerFor(player);
        }

        viewers.removeIf(viewerId -> Bukkit.getPlayer(viewerId) == null);

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            despawnAll();
        }
    }

    private boolean isInViewRange(final Player player) {
        final World playerWorld = player.getWorld();

        for (final Node node : nodes) {
            if (!node.worldId().equals(playerWorld.getUID())) {
                continue;
            }

            final double dx = player.getLocation().getX() - (node.x() + 0.5D);
            final double dy = player.getLocation().getY() - (node.y() + 0.5D);
            final double dz = player.getLocation().getZ() - (node.z() + 0.5D);
            final double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);

            if (distanceSquared <= VIEW_DISTANCE_SQUARED) {
                return true;
            }
        }

        return false;
    }

    public void updateViewerFor(final Player player) {
        final boolean shouldSee = isInViewRange(player);
        final boolean isViewer = viewers.contains(player.getUniqueId());

        if (shouldSee && !isViewer) {
            showFor(player);
            return;
        }

        if (!shouldSee && isViewer) {
            hideFor(player);
        }
    }

    private void startRotationTask() {
        rotationTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                new Runnable() {
                    private float angle = 0f;

                    @Override
                    public void run() {
                        angle += 5f;
                        final float rad = (float) Math.toRadians(angle);

                        for (final BlockDisplay display : nodeDisplays.values()) {
                            if (!display.isValid()) {
                                continue;
                            }
                            final Transformation t = display.getTransformation();
                            final Quaternionf left = new Quaternionf().rotateY(rad);

                            display.setTransformation(new Transformation(
                                    t.getTranslation(),
                                    left,
                                    t.getScale(),
                                    t.getRightRotation()
                            ));
                        }
                    }
                },
                0L,
                2L
        );
    }

    @Override
    public void showFor(final Player player) {
        viewers.add(player.getUniqueId());

        for (final BlockDisplay display : nodeDisplays.values()) {
            if (!display.isValid()) {
                continue;
            }
            player.showEntity(plugin, display);
        }

        for (final BlockDisplay display : edgeDisplays.values()) {
            if (!display.isValid()) {
                continue;
            }
            player.showEntity(plugin, display);
        }
    }

    @Override
    public void hideFor(final Player player) {
        viewers.remove(player.getUniqueId());

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
        if (rotationTaskId != -1) {
            Bukkit.getScheduler().cancelTask(rotationTaskId);
            rotationTaskId = -1;
        }

        if (visibilityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(visibilityTaskId);
            visibilityTaskId = -1;
        }

        despawnAll();
        viewers.clear();
    }

    @Override
    public boolean isViewer(final Player player) {
        return viewers.contains(player.getUniqueId());
    }
}
