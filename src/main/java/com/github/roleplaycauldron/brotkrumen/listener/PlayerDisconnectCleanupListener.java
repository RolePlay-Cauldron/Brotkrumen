package com.github.roleplaycauldron.brotkrumen.listener;

import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveGuidanceSessionManager;
import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Cleans player-scoped runtime state when a player disconnects.
 */
public final class PlayerDisconnectCleanupListener implements Listener {

    private final ResolveGuidanceSessionManager resolveSessions;

    private final EditorService editorService;

    private final VisualizerRegistry visualizerRegistry;

    /**
     * Creates a disconnect cleanup listener.
     *
     * @param resolveSessions    resolve session manager
     * @param editorService      editor session service
     * @param visualizerRegistry visualizer registry
     */
    public PlayerDisconnectCleanupListener(final ResolveGuidanceSessionManager resolveSessions,
                                           final EditorService editorService,
                                           final VisualizerRegistry visualizerRegistry) {
        this.resolveSessions = resolveSessions;
        this.editorService = editorService;
        this.visualizerRegistry = visualizerRegistry;
    }

    /**
     * Handles player disconnect cleanup.
     *
     * @param event quit event
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final UUID playerId = event.getPlayer().getUniqueId();
        resolveSessions.cancel(playerId);
        editorService.cancel(playerId);
        visualizerRegistry.unregisterDisconnected(playerId);
    }
}
