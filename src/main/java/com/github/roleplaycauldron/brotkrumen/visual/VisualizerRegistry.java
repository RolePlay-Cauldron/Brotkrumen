package com.github.roleplaycauldron.brotkrumen.visual;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the lifecycle and visibility updates of {@link GraphVisualizer} instances registered for players.
 * Responsible for registering, unregistering, and periodically updating the visibility of visualizers
 * associated with player UUIDs.
 */
public class VisualizerRegistry {

    private static final long VISIBILITY_CHECK_PERIOD_TICKS = 10L;

    private final Brotkrumen plugin;

    private final WrappedLogger log;

    private final Map<UUID, GraphVisualizer> visualisers;

    private int visibilityTaskId;

    /**
     * Initializes a new instance of the {@code VisualizerRegistry} class.
     * This registry is responsible for managing the lifecycle of visualizers,
     * including their registration, unregistration, and periodic visibility updates.
     *
     * @param plugin the instance of {@code Brotkrumen}, which provides the plugin context
     *               required for managing visualizer life cycles and scheduling tasks
     * @param log    the {@link WrappedLogger} instance for logging messages related to visualizer operations
     */
    public VisualizerRegistry(final Brotkrumen plugin, final WrappedLogger log) {
        this.plugin = plugin;
        this.log = log;
        visualisers = new HashMap<>();
        this.visibilityTaskId = -1;
    }

    /**
     * Registers a {@link GraphVisualizer} instance with the associated UUID and updates its visibility.
     * This method stores the provided visualizer in the registry and immediately triggers a visibility update
     * to ensure the visualizer's state is synchronized upon registration.
     *
     * @param uuid       the {@link UUID} of the player or entity for which the visualizer is being registered
     * @param visualiser the {@link GraphVisualizer} instance to be registered and managed
     */
    public void register(final UUID uuid, final GraphVisualizer visualiser) {
        visualisers.put(uuid, visualiser);
        visualiser.visibilityUpdate();
    }

    /**
     * Unregisters and shuts down the {@link GraphVisualizer} instance associated with the specified UUID.
     * This method removes the visualizer from the registry and invokes its {@code shutdown()} method to
     * ensure any resources are released and operations are safely terminated.
     *
     * @param uuid the {@link UUID} of the player or entity whose visualizer is being unregistered
     */
    public void unregister(final UUID uuid) {
        if (visualisers.containsKey(uuid)) {
            visualisers.get(uuid).shutdown();
            visualisers.remove(uuid);
        } else {
            log.infoF("Attempted to unregister non-existent visualizer for UUID '%s'", uuid);
        }
    }

    /**
     * Refreshes one active visualizer from its visual graph source.
     *
     * @param uuid viewer uuid
     */
    public void refresh(final UUID uuid) {
        final GraphVisualizer visualiser = visualisers.get(uuid);
        if (visualiser != null) {
            visualiser.refresh();
        }
    }

    /**
     * Refreshes all active visualizers from their visual graph sources.
     */
    public void refreshAll() {
        new HashMap<>(visualisers).values().forEach(GraphVisualizer::refresh);
    }

    /**
     * Starts the periodic updates for managing the visibility of registered visualizers.
     * <p>
     * This method schedules a repeating task using the Bukkit scheduler to periodically
     * evaluate the visibility of all visualizers in the registry. The visibility updates
     * ensure that visualizers remain in sync with the current state of players or entities
     * and perform the necessary cleanup, such as shutting down and removing visualizers
     * associated with offline players or entities.
     * <p>
     * The task executes repeatedly with a delay specified by {@code VISIBILITY_CHECK_PERIOD_TICKS}.
     * If this method is invoked multiple times without calling {@code stopVisibilityUpdates()},
     * redundant tasks may be scheduled, which can result in inefficiencies or unintended behavior.
     * It is recommended to ensure no duplicate scheduling occurs.
     */
    public void startVisibilityUpdates() {
        visibilityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::visibilityUpdate,
                0L,
                VISIBILITY_CHECK_PERIOD_TICKS
        );
    }

    /**
     * Stops the periodic visibility updates for registered visualizers.
     * <p>
     * This method cancels the currently scheduled visibility update task, if one exists,
     * and resets the task identifier to indicate that no task is active.
     * It ensures that unnecessary updates do not continue when visibility checks are
     * no longer required or the registry is being shut down.
     * <p>
     * This method should be called as part of the cleanup process to release resources
     * and prevent unintended behavior resulting from redundant task execution.
     */
    public void stopVisibilityUpdates() {
        if (visibilityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(visibilityTaskId);
            visibilityTaskId = -1;
        }
    }

    private void visibilityUpdate() {
        final Map<UUID, GraphVisualizer> copiedVisualiser = new HashMap<>(visualisers);
        copiedVisualiser.forEach((key, vis) -> {
            if (plugin.getServer().getPlayer(key) == null) {
                vis.shutdown();
                visualisers.remove(key);
                return;
            }
            vis.visibilityUpdate();
        });
    }
}
