package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Applies configured player-facing effects when guided resolve reaches its goal.
 */
public final class ResolveGoalNotifier {

    private static final String MESSAGE_KEY = "commands.bk.resolve.status.guidanceComplete";

    private static final String TITLE_KEY = "commands.bk.resolve.status.guidanceCompleteTitle";

    private static final String SUBTITLE_KEY = "commands.bk.resolve.status.guidanceCompleteSubtitle";

    private static final long MILLIS_PER_TICK = 50L;

    private final Localization localization;

    /**
     * Creates a notifier.
     *
     * @param localization localization service
     */
    public ResolveGoalNotifier(final Localization localization) {
        this.localization = localization;
    }

    /**
     * Notifies a player about resolve goal completion.
     *
     * @param player  player to notify
     * @param options completion effect options
     */
    public void notify(final Player player, final ResolveGoalOptions options) {
        if (player == null || options == null) {
            return;
        }
        if (options.messageEnabled() && localization != null) {
            player.sendMessage(localization.getPrefixedMessage(MESSAGE_KEY));
        }
        playSound(player, options);
        showTitle(player, options);
    }

    private void playSound(final Player player, final ResolveGoalOptions options) {
        if (!options.soundEnabled() || !validSoundKey(options.soundName())) {
            return;
        }
        final Location location = player.getLocation();
        if (location != null) {
            player.playSound(location, options.soundName(), options.soundVolume(), options.soundPitch());
        }
    }

    private boolean validSoundKey(final String soundName) {
        return NamespacedKey.fromString(soundName) != null;
    }

    private void showTitle(final Player player, final ResolveGoalOptions options) {
        if (!options.titleEnabled() || localization == null) {
            return;
        }
        final Component title = localization.getFormattedMessage(TITLE_KEY);
        final Component subtitle = localization.getFormattedMessage(SUBTITLE_KEY);
        player.showTitle(Title.title(title, subtitle, Title.Times.times(
                ticks(options.titleFadeInTicks()),
                ticks(options.titleStayTicks()),
                ticks(options.titleFadeOutTicks())
        )));
    }

    private Duration ticks(final int ticks) {
        return Duration.ofMillis(ticks * MILLIS_PER_TICK);
    }
}
