package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Repeats editor actionbar guidance for players waiting to continue placement.
 */
public final class EditorWaitingActionBarReminder {

    private static final long PERIOD_TICKS = 20L;

    private final EditorService editorService;

    private final Predicate<Player> suppressed;

    private Localization localization;

    /**
     * Creates a reminder sender.
     *
     * @param editorService editor state provider
     */
    public EditorWaitingActionBarReminder(final EditorService editorService) {
        this(editorService, player -> false);
    }

    /**
     * Creates a reminder sender that stays silent for some players.
     *
     * @param editorService editor state provider
     * @param suppressed    true for players who should not receive the reminder, for example while they hold the wand
     */
    public EditorWaitingActionBarReminder(final EditorService editorService, final Predicate<Player> suppressed) {
        this.editorService = editorService;
        this.suppressed = suppressed;
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
            if (player != null && !suppressed.test(player)) {
                player.sendActionBar(localization == null
                        ? net.kyori.adventure.text.Component.empty()
                        : localization.getFormattedMessage(EditorService.waitingAnchorActionBarMessage()));
            }
        }
    }
}
