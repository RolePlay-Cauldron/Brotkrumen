package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSegment;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Executes selected teleport-like guided resolve path segments.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class ResolveAutoTeleportController {

    private static final long NO_COOLDOWN = 0L;

    private static final long NO_DELAY_TICKS = 0L;

    private final GraphNetwork network;

    private final PathResult path;

    private final GuidedPathVisualGraphSource source;

    private final ResolveAutoTeleportOptions options;

    private final Supplier<Player> playerSupplier;

    private final DelayScheduler scheduler;

    private final LongSupplier currentTick;

    private final BooleanSupplier currentSession;

    private final Localization localization;

    private PendingTeleport pendingTeleport;

    private long cooldownUntilTick;

    private int nextSegmentIndex;

    /**
     * Creates a controller.
     */
    public ResolveAutoTeleportController(final GraphNetwork network,
                                         final PathResult path,
                                         final GuidedPathVisualGraphSource source,
                                         final ResolveAutoTeleportOptions options,
                                         final Supplier<Player> playerSupplier,
                                         final DelayScheduler scheduler,
                                         final LongSupplier currentTick,
                                         final BooleanSupplier currentSession,
                                         final Localization localization) {
        this.network = Objects.requireNonNull(network, "network");
        this.path = path == null ? PathResult.empty() : path;
        this.source = Objects.requireNonNull(source, "source");
        this.options = options == null ? ResolveAutoTeleportOptions.defaults() : options;
        this.playerSupplier = Objects.requireNonNull(playerSupplier, "playerSupplier");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
        this.currentSession = Objects.requireNonNull(currentSession, "currentSession");
        this.localization = localization;
        this.cooldownUntilTick = NO_COOLDOWN;
        this.nextSegmentIndex = 0;
    }

    /**
     * Updates pending and trigger state.
     */
    public void tick() {
        if (!options.enabled() || !currentSession.getAsBoolean()) {
            cancelPending(false);
            return;
        }
        if (hasPendingTeleport()) {
            validatePending();
            return;
        }
        if (cooldownActive()) {
            return;
        }
        final TriggerCandidate candidate = triggerCandidate();
        if (candidate == null) {
            return;
        }
        final Player player = playerSupplier.get();
        if (player == null) {
            return;
        }
        final Node sourceNode = network.getNode(candidate.segment().source());
        if (sourceNode == null || !isInRange(player.getLocation(), sourceNode, options.cancelRange())) {
            return;
        }
        trigger(candidate.segmentIndex(), candidate.segment());
    }

    private boolean hasPendingTeleport() {
        return pendingTeleport != null;
    }

    private boolean cooldownActive() {
        return currentTick.getAsLong() < cooldownUntilTick;
    }

    private TriggerCandidate triggerCandidate() {
        final int segmentIndex = nextEligibleSegmentIndex();
        final int progressIndex = source.currentProgressIndex();
        if (segmentIndex < 0 || progressIndex < segmentIndex) {
            return null;
        }
        if (progressIndex > segmentIndex) {
            nextSegmentIndex = segmentIndex + 1;
            return null;
        }
        return new TriggerCandidate(segmentIndex, path.segments().get(segmentIndex));
    }

    private int nextEligibleSegmentIndex() {
        for (int index = nextSegmentIndex; index < path.segments().size(); index++) {
            if (isEligible(path.segments().get(index).traversalKind())) {
                return index;
            }
        }
        return -1;
    }

    private boolean isEligible(final TraversalKind traversalKind) {
        return switch (traversalKind) {
            case LOCAL_TELEPORT -> options.localTeleportEnabled();
            case INTERGRAPH_TELEPORT -> options.interGraphTeleportEnabled();
            case WARP -> options.warpEnabled();
            default -> false;
        };
    }

    private void trigger(final int segmentIndex, final PathSegment segment) {
        if (options.delayTicks() <= NO_DELAY_TICKS) {
            execute(segmentIndex, segment);
            return;
        }
        final ScheduledHandle handle = scheduler.schedule(options.delayTicks(), () -> execute(segmentIndex, segment));
        pendingTeleport = new PendingTeleport(segment.source(), handle);
        if (options.messageEnabled() && localization != null) {
            final Player player = playerSupplier.get();
            if (player != null) {
                player.sendMessage(localization.getPrefixedMessage("commands.bk.resolve.status.autoTeleportPending",
                        Map.of("seconds", Integer.toString(options.delaySeconds()))));
            }
        }
    }

    private void validatePending() {
        if (!options.cancelWhenPlayerMovesAway()) {
            return;
        }
        final Player player = playerSupplier.get();
        final Node sourceNode = network.getNode(pendingTeleport.source());
        if (player == null || sourceNode == null || !isInRange(player.getLocation(), sourceNode, options.cancelRange())) {
            cancelPending(true);
        }
    }

    private void cancelPending(final boolean notify) {
        if (pendingTeleport == null) {
            return;
        }
        pendingTeleport.handle().cancel();
        pendingTeleport = null;
        applyCooldown();
        if (notify && localization != null) {
            final Player player = playerSupplier.get();
            if (player != null) {
                final Component message = localization.getPrefixedMessage("commands.bk.resolve.status.autoTeleportCancelled");
                player.sendMessage(message);
            }
        }
    }

    private void execute(final int segmentIndex, final PathSegment segment) {
        if (!currentSession.getAsBoolean()) {
            return;
        }
        pendingTeleport = null;
        final Player player = playerSupplier.get();
        final Node target = network.getNode(segment.target());
        if (player == null || target == null) {
            return;
        }
        player.teleport(target.toCenterLocation());
        nextSegmentIndex = segmentIndex + 1;
        applyCooldown();
    }

    private void applyCooldown() {
        cooldownUntilTick = currentTick.getAsLong() + options.cooldownTicks();
    }

    private boolean isInRange(final Location location, final Node node, final double range) {
        if (location == null) {
            return false;
        }
        if (location.getWorld() != null && node.worldId() != null && !location.getWorld().getUID().equals(node.worldId())) {
            return false;
        }
        final double deltaX = location.getX() - (node.x() + 0.5D);
        final double deltaY = location.getY() - (node.y() + 0.5D);
        final double deltaZ = location.getZ() - (node.z() + 0.5D);
        return ((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ)) <= range * range;
    }

    /**
     * Schedules delayed controller work.
     */
    @FunctionalInterface
    public interface DelayScheduler {
        /**
         * Schedules a delayed action.
         *
         * @param delayTicks delay in ticks
         * @param action     action
         * @return scheduled handle
         */
        ScheduledHandle schedule(long delayTicks, Runnable action);
    }

    /**
     * Cancellable scheduled work.
     */
    @FunctionalInterface
    public interface ScheduledHandle {
        /**
         * Cancels scheduled work.
         */
        void cancel();
    }

    private record PendingTeleport(NodeRef source, ScheduledHandle handle) {
    }

    private record TriggerCandidate(int segmentIndex, PathSegment segment) {
    }
}
