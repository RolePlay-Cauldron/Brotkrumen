package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockDisplayVisualiser implements NodeLayer {

    private final Brotkrumen plugin;

    private final Collection<Node> nodes;

    private final Collection<Edge> edges;

    private final Map<UUID, BlockDisplay> displays = new HashMap<>();

    private final Set<UUID> viewers = new HashSet<>();

    private int rotationTaskId = -1;

    public BlockDisplayVisualiser(final Brotkrumen plugin, final Collection<Node> nodes, final Collection<Edge> edges) {
        this.plugin = plugin;
        this.nodes = nodes;
        this.edges = edges;

        spawnAll();
        startRotationTask();
    }

    private void spawnAll() {
        for (final Node node : nodes) {
            final Location loc = node.toCenterLocation();
            if (loc.getWorld() == null) {
                plugin.getLogger().warning("Node is in an unloaded or invalid world: " + node.worldId());
                continue;
            }

            final BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class, entity -> {
                entity.setBlock(Material.COAL_BLOCK.createBlockData());
                entity.setPersistent(false);
            });
            displays.put(node.graphId(), display);
        }

        for (final Player p : Bukkit.getOnlinePlayers()) {
            hideFor(p);
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
                            if (!display.isValid()) continue;
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
            if (!display.isValid()) continue;
            player.showEntity(plugin, display);
        }
    }

    @Override
    public void hideFor(final Player player) {
        viewers.remove(player.getUniqueId());

        for (final BlockDisplay display : displays.values()) {
            if (!display.isValid()) continue;
            player.hideEntity(plugin, display);
        }
    }

    @Override
    public void shutdown() {
        if (rotationTaskId != -1) {
            Bukkit.getScheduler().cancelTask(rotationTaskId);
            rotationTaskId = -1;
        }

        for (final BlockDisplay display : displays.values()) {
            if (display.isValid()) display.remove();
        }

        displays.clear();
        viewers.clear();
    }

    @Override
    public boolean isViewer(final Player player) {
        return viewers.contains(player.getUniqueId());
    }
}
