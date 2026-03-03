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

    private static final float DISPLAY_HALF_EXTENT = DISPLAY_SCALE / 2.0f;

    private static final double VIEW_DISTANCE = 48.0D;

    private static final double VIEW_DISTANCE_SQUARED = VIEW_DISTANCE * VIEW_DISTANCE;

    private static final long VISIBILITY_CHECK_PERIOD_TICKS = 10L;

    private final Brotkrumen plugin;

    private final Collection<Node> nodes;

    private final Collection<Edge> edges;

    private final Map<UUID, BlockDisplay> displays = new HashMap<>();

    private final Set<UUID> viewers = new HashSet<>();

    private int rotationTaskId = -1;

    private int visibilityTaskId = -1;

    public BlockDisplayVisualiser(final Brotkrumen plugin, final Collection<Node> nodes, final Collection<Edge> edges) {
        this.plugin = plugin;
        this.nodes = nodes;
        this.edges = edges;

        if (edges.isEmpty()) {
            plugin.getLogger().warning("No edges configured for BlockDisplayVisualiser graph.");
        }

        startVisibilityTask();
        startRotationTask();
    }

    private void spawnAll() {
        if (!displays.isEmpty()) {
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
            displays.put(node.graphId(), display);
        }
    }

    private void despawnAll() {
        for (final BlockDisplay display : displays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }

        displays.clear();
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
            if (displays.isEmpty()) {
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

                        for (final BlockDisplay display : displays.values()) {
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

        for (final BlockDisplay display : displays.values()) {
            if (!display.isValid()) {
                continue;
            }
            player.showEntity(plugin, display);
        }
    }

    @Override
    public void hideFor(final Player player) {
        viewers.remove(player.getUniqueId());

        for (final BlockDisplay display : displays.values()) {
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
