package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VisualiserRegistry {

    private static final long VISIBILITY_CHECK_PERIOD_TICKS = 10L;

    private final Brotkrumen plugin;

    private final Map<UUID, GraphVisualiser> visualisers;

    private int visibilityTaskId;

    public VisualiserRegistry(final Brotkrumen plugin) {
        this.plugin = plugin;
        visualisers = new HashMap<>();
        this.visibilityTaskId = -1;
    }

    public void register(final UUID uuid, final GraphVisualiser visualiser) {
        visualisers.put(uuid, visualiser);
        visualiser.visibilityUpdate();
    }

    public void unregister(final UUID uuid) {
        visualisers.remove(uuid);
    }

    public void startVisibilityUpdates() {
        visibilityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::updateAllViewers,
                0L,
                VISIBILITY_CHECK_PERIOD_TICKS
        );
    }

    public void stopVisibilityUpdates() {
        if (visibilityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(visibilityTaskId);
            visibilityTaskId = -1;
        }
    }

    private void updateAllViewers() {
        final Map<UUID, GraphVisualiser> copiedVisualiser = new HashMap<>(visualisers);
        copiedVisualiser.forEach((key, vis) -> {
            if (vis.viewers.isEmpty()) {
                vis.shutdown();
                visualisers.remove(key);
                return;
            }
            vis.visibilityUpdate();
        });
    }
}
