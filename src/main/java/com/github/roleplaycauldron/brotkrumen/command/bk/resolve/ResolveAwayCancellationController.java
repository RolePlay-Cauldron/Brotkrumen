package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Cancels guided resolve sessions when the player remains away from the guided route.
 */
public final class ResolveAwayCancellationController {

    private static final long NO_GRACE_TICKS = 0L;

    private final GuidedPathVisualGraphSource source;

    private final ResolveAwayCancellationOptions options;

    private final Supplier<Player> playerSupplier;

    private final LongSupplier currentTick;

    private final BooleanSupplier currentSession;

    private final Runnable cancelSession;

    private final Localization localization;

    private Long awayDeadlineTick;

    /**
     * Creates a controller.
     *
     * @param source         the guided path source
     * @param options        away-cancellation options
     * @param playerSupplier player supplier
     * @param currentTick    current tick supplier
     * @param currentSession current session status supplier
     * @param cancelSession  token-safe cancellation action
     * @param localization   localization service
     */
    public ResolveAwayCancellationController(final GuidedPathVisualGraphSource source,
                                             final ResolveAwayCancellationOptions options,
                                             final Supplier<Player> playerSupplier,
                                             final LongSupplier currentTick,
                                             final BooleanSupplier currentSession,
                                             final Runnable cancelSession,
                                             final Localization localization) {
        this.source = Objects.requireNonNull(source, "source");
        this.options = options == null ? ResolveAwayCancellationOptions.defaults() : options;
        this.playerSupplier = Objects.requireNonNull(playerSupplier, "playerSupplier");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
        this.currentSession = Objects.requireNonNull(currentSession, "currentSession");
        this.cancelSession = Objects.requireNonNull(cancelSession, "cancelSession");
        this.localization = localization;
    }

    /**
     * Updates away-cancellation state.
     */
    public void tick() {
        if (inactive()) {
            awayDeadlineTick = null;
            return;
        }
        final Player player = playerSupplier.get();
        if (player == null || playerInRange()) {
            awayDeadlineTick = null;
            return;
        }
        handleAway(player);
    }

    private boolean inactive() {
        return !options.enabled() || !currentSession.getAsBoolean() || source.complete();
    }

    private boolean playerInRange() {
        return source.viewerWithinCurrentPath(options.distance());
    }

    private void handleAway(final Player player) {
        if (awayDeadlineTick == null) {
            startGracePeriod(player);
            if (options.warningGraceTicks() > NO_GRACE_TICKS) {
                return;
            }
        }
        if (currentTick.getAsLong() >= awayDeadlineTick) {
            cancel(player);
        }
    }

    private void startGracePeriod(final Player player) {
        awayDeadlineTick = currentTick.getAsLong() + options.warningGraceTicks();
        warn(player);
    }

    private void warn(final Player player) {
        if (options.warningEnabled() && localization != null) {
            player.sendMessage(localization.getPrefixedMessage("commands.bk.resolve.status.cancelWarning"));
        }
    }

    private void cancel(final Player player) {
        awayDeadlineTick = null;
        cancelSession.run();
        if (localization != null) {
            player.sendMessage(localization.getPrefixedMessage("commands.bk.resolve.status.cancelledAway"));
        }
    }
}
