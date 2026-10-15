package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.command.editor.EditorCommandFeedback;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
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

    private final EditorCommandFeedback feedback;

    /**
     * Players whose walking-based placement is currently paused because they hold the wand.
     */
    private final Set<UUID> pausedByWand = new HashSet<>();

    /**
     * Constructs a new WalkingListener.
     *
     * @param log           the logger used for logging information
     * @param editorService the service responsible for handling editor-related functionality
     * @param wandItems     wand item helper
     * @param wandSettings  supplier of the current wand settings
     * @param feedback      editor feedback renderer
     */
    public WalkingListener(final WrappedLogger log, final EditorService editorService, final WandItems wandItems,
                           final Supplier<WandSettings> wandSettings, final EditorCommandFeedback feedback) {
        this.log = log;
        this.editorService = editorService;
        this.wandItems = wandItems;
        this.wandSettings = wandSettings;
        this.feedback = feedback;
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

        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (!editorService.isEditing(uuid)) {
            pausedByWand.remove(uuid);
            return;
        }
        if (!wandSettings.get().autoPlacementWhileHeld() && holdsWand(player)) {
            pausedByWand.add(uuid);
            return;
        }
        if (pausedByWand.remove(uuid)) {
            // If the player walked away from the last node while placement was paused for the wand, resuming
            // directly would connect that node to the current position, so ask for an anchor first.
            feedback.send(player, editorService.pauseForAppendAnchor(uuid, player.getLocation()));
        }

        final EditorService.EditorResult result = editorService.handleMovement(uuid, player.getLocation());
        if (result.success() && !result.message().isBlank()) {
            log.infoF("Editor movement for %s: %s", player.getName(), result.message());
        }
    }

    /**
     * Forgets the wand pause state of players who leave.
     *
     * @param event quit event
     */
    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        pausedByWand.remove(event.getPlayer().getUniqueId());
    }

    private boolean holdsWand(final Player player) {
        return wandItems.isWand(player.getInventory().getItemInMainHand())
                || wandItems.isWand(player.getInventory().getItemInOffHand());
    }
}
