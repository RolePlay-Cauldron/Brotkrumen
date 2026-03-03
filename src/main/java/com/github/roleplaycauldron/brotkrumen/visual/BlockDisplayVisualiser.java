package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
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

public class BlockDisplayVisualiser implements NodeLayer {

    private static final float DISPLAY_SCALE = 0.4f;

    private static final float EDGE_THICKNESS = 0.15f;

    private static final double EDGE_NODE_CLEARANCE = 0.35D;

    private static final double MIN_EDGE_LENGTH = 1.0D;

    private static final float DISPLAY_HALF_EXTENT = DISPLAY_SCALE / 2.0f;

    private static final double VIEW_DISTANCE = 48.0D;

    private static final double VIEW_DISTANCE_SQUARED = VIEW_DISTANCE * VIEW_DISTANCE;

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

    private void spawnAll() {
        if (!nodeDisplays.isEmpty() || !edgeDisplays.isEmpty()) {
            return;
        }

        for (final Node node : nodes) {
            final Location loc = node.toCenterLocation();
            if (loc.getWorld() == null) {
                plugin.getLogger().warning("Node is in an unloaded or invalid world: " + node.worldId());
                continue;
            }

            final BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class, entity -> {
                entity.setBlock(Material.COAL_BLOCK.createBlockData());
                entity.setPersistent(false);
                entity.setTransformation(new Transformation(
                        new Vector3f(-DISPLAY_HALF_EXTENT, -DISPLAY_HALF_EXTENT, -DISPLAY_HALF_EXTENT),
                        new Quaternionf(),
                        new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                        new Quaternionf()
                ));
            });
            nodeDisplays.put(node.graphId(), display);
        }

        for (final Edge edge : edges) {
            final Node source = nodesById.get(edge.source());
            final Node target = nodesById.get(edge.target());

            if (source == null || target == null) {
                plugin.getLogger().warning("Edge references unknown node: " + edge.edgeId());
                continue;
            }

            if (!source.worldId().equals(target.worldId())) {
                plugin.getLogger().warning("Edge spans worlds and cannot be visualised: " + edge.edgeId());
                continue;
            }

            final World world = Bukkit.getWorld(source.worldId());
            if (world == null) {
                plugin.getLogger().warning("Edge world is unloaded or invalid: " + edge.edgeId());
                continue;
            }

            final Vector3f sourceCenter = new Vector3f(source.x() + 0.5f, source.y() + 0.5f, source.z() + 0.5f);
            final Vector3f targetCenter = new Vector3f(target.x() + 0.5f, target.y() + 0.5f, target.z() + 0.5f);
            final Vector3f direction = new Vector3f(targetCenter).sub(sourceCenter);
            final double distance = direction.length();

            if (distance == 0.0D) {
                continue;
            }

            final double maxLength = distance - (2.0D * EDGE_NODE_CLEARANCE);
            if (maxLength <= 0.0D) {
                continue;
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

            final float renderedLength = (float) displayLength;
            final Quaternionf rotation = new Quaternionf().rotateTo(0.0f, 0.0f, 1.0f, direction.x, direction.y, direction.z);
            final BlockDisplay edgeDisplay = world.spawn(displayLoc, BlockDisplay.class, entity -> {
                entity.setBlock(Material.GLASS_PANE.createBlockData());
                entity.setPersistent(false);
                entity.setTransformation(new Transformation(
                        new Vector3f(-EDGE_THICKNESS / 2.0f, -EDGE_THICKNESS / 2.0f, 0.0f),
                        rotation,
                        new Vector3f(EDGE_THICKNESS, EDGE_THICKNESS, renderedLength),
                        new Quaternionf()
                ));
            });
            edgeDisplays.put(edge.edgeId(), edgeDisplay);
        }
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
        for (final Player player : Bukkit.getOnlinePlayers()) {
            updateViewerFor(player);
        }

        viewers.removeIf(viewerId -> Bukkit.getPlayer(viewerId) == null);

        if (viewers.isEmpty()) {
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
            if (nodeDisplays.isEmpty() && edgeDisplays.isEmpty()) {
                spawnAll();
            }
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
