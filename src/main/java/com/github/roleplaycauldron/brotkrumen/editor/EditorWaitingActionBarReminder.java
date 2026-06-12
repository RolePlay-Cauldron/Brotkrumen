package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Function;

/**
 * Repeats editor actionbar guidance for players waiting to continue placement.
 */
public final class EditorWaitingActionBarReminder {

    private static final long PERIOD_TICKS = 20L;

    private final EditorService editorService;

    private Localization localization;

    /**
     * Creates a reminder sender.
     *
     * @param editorService editor state provider
     */
    public EditorWaitingActionBarReminder(final EditorService editorService) {
        this.editorService = editorService;
    }

    /**
     * Starts periodic guidance for active waiting editor sessions.
     *
     * @param plugin plugin scheduler owner
     */
    public void start(final JavaPlugin plugin) {
        if (plugin instanceof final Brotkrumen brotkrumen) {
            localization = brotkrumen.getLocalization();
        }
        plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> sendWaitingAnchorActionBars(playerId -> plugin.getServer().getPlayer(playerId)),
                PERIOD_TICKS, PERIOD_TICKS);
    }

    /* default */ void sendWaitingAnchorActionBars(final Function<UUID, Player> playerLookup) {
        for (final UUID playerId : editorService.editorPlayerIds()) {
            if (!editorService.isWaitingForAppendAnchor(playerId)) {
                continue;
            }
            final Player player = playerLookup.apply(playerId);
            if (player != null) {
                player.sendActionBar(localization == null
                        ? net.kyori.adventure.text.Component.empty()
                        : localization.getFormattedMessage(EditorService.waitingAnchorActionBarMessage()));
            }
        }
    }
}
