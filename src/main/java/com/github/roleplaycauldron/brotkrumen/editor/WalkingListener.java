package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Listener for player walking events during editor mode.
 */
public class WalkingListener implements Listener {

    private final WrappedLogger log;

    private final EditorService editorService;

    private final WandItems wandItems;

    private final Supplier<WandSettings> wandSettings;

    /**
     * Constructs a new WalkingListener.
     *
     * @param log           the logger used for logging information
     * @param editorService the service responsible for handling editor-related functionality
     * @param wandItems     wand item helper
     * @param wandSettings  supplier of the current wand settings
     */
    public WalkingListener(final WrappedLogger log, final EditorService editorService, final WandItems wandItems,
                           final Supplier<WandSettings> wandSettings) {
        this.log = log;
        this.editorService = editorService;
        this.wandItems = wandItems;
        this.wandSettings = wandSettings;
    }

    /**
     * Handles player movement events during editor mode.
     *
     * @param event the player move event
     */
    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final UUID uuid = event.getPlayer().getUniqueId();
        if (!editorService.isEditing(uuid)) {
            return;
        }
        if (!wandSettings.get().autoPlacementWhileHeld() && holdsWand(event.getPlayer())) {
            return;
        }

        final EditorService.EditorResult result = editorService.handleMovement(uuid, event.getPlayer().getLocation());
        if (result.success() && !result.message().isBlank()) {
            log.infoF("Editor movement for %s: %s", event.getPlayer().getName(), result.message());
        }
    }

    private boolean holdsWand(final Player player) {
        return wandItems.isWand(player.getInventory().getItemInMainHand())
                || wandItems.isWand(player.getInventory().getItemInOffHand());
    }
}
