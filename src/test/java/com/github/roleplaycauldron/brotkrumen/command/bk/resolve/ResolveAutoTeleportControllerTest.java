package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSegment;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.visual.source.GraphNetworkVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResolveAutoTeleportController}.
 */
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.CouplingBetweenObjects"})
class ResolveAutoTeleportControllerTest {

    private static final UUID WORLD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void executesSelectedLocalTeleportSegment() {
        final Harness harness = harness(TraversalKind.LOCAL_TELEPORT, ResolveAutoTeleportOptions.defaults());

        try (MockedStatic<Bukkit> bukkit = mockBukkitWorld(harness.world())) {
            harness.reachSource();
            harness.controller().tick();

            verify(harness.player()).teleport(org.mockito.ArgumentMatchers.<Location>argThat(location -> location.getX() == 10.5D
                    && location.getY() == 64.5D && location.getZ() == 0.5D));
            bukkit.verify(() -> Bukkit.getWorld(WORLD_ID));
        }
    }

    @Test
    void ignoresTeleportWhenTypeToggleDisabled() {
        final ResolveAutoTeleportOptions options = new ResolveAutoTeleportOptions(true, 0, true, 0, true,
                5.0D, false, true, true, true);
        final Harness harness = harness(TraversalKind.LOCAL_TELEPORT, options);

        harness.reachSource();
        harness.controller().tick();

        verify(harness.player(), never()).teleport(any(Location.class));
    }

    @Test
    void ignoresNormalSegments() {
        final Harness harness = harness(TraversalKind.NORMAL, ResolveAutoTeleportOptions.defaults());

        harness.reachSource();
        harness.controller().tick();

        verify(harness.player(), never()).teleport(any(Location.class));
    }

    @Test
    void schedulesDelayedTeleportAndExecutesAfterCountdown() {
        final ResolveAutoTeleportOptions options = new ResolveAutoTeleportOptions(true, 2, false, 0, true,
                5.0D, true, true, true, true);
        final Harness harness = harness(TraversalKind.WARP, options);

        harness.reachSource();
        harness.controller().tick();

        assertEquals(40L, harness.scheduler().lastScheduledDelayTicks(), "Two seconds should schedule forty ticks");
        verify(harness.player(), never()).teleport(any(Location.class));

        try (MockedStatic<Bukkit> ignored = mockBukkitWorld(harness.world())) {
            harness.scheduler().runScheduled();
            verify(harness.player()).teleport(any(Location.class));
        }
    }

    @Test
    void cancelsPendingTeleportWhenPlayerMovesOutOfRangeButKeepsSessionUsable() {
        final ResolveAutoTeleportOptions options = new ResolveAutoTeleportOptions(true, 2, false, 0, true,
                5.0D, true, true, true, true);
        final Harness harness = harness(TraversalKind.INTERGRAPH_TELEPORT, options);

        harness.reachSource();
        harness.controller().tick();
        harness.movePlayer(new Location(harness.world(), 20.0D, 64.0D, 0.0D));
        harness.controller().tick();

        assertTrue(harness.scheduler().lastHandleCancelled(), "Pending task should be cancelled");
        verify(harness.player(), never()).teleport(any(Location.class));

        harness.movePlayer(new Location(harness.world(), 0.5D, 64.5D, 0.5D));
        harness.controller().tick();

        assertEquals(2, harness.scheduler().scheduledCount(), "Session should allow the teleport to be scheduled again");
    }

    @Test
    void cooldownPreventsImmediateRetriggerAfterCancellation() {
        final ResolveAutoTeleportOptions options = new ResolveAutoTeleportOptions(true, 2, false, 3, true,
                5.0D, true, true, true, true);
        final Harness harness = harness(TraversalKind.LOCAL_TELEPORT, options);

        harness.reachSource();
        harness.controller().tick();
        harness.movePlayer(new Location(harness.world(), 20.0D, 64.0D, 0.0D));
        harness.controller().tick();
        harness.movePlayer(new Location(harness.world(), 0.5D, 64.5D, 0.5D));
        harness.controller().tick();

        assertEquals(1, harness.scheduler().scheduledCount(), "Cooldown should block immediate retrigger");

        harness.advanceTicks(60L);
        harness.controller().tick();

        assertEquals(2, harness.scheduler().scheduledCount(), "Retrigger should be allowed after cooldown");
    }

    @Test
    void staleSessionDoesNotExecuteDelayedTeleport() {
        final ResolveAutoTeleportOptions options = new ResolveAutoTeleportOptions(true, 1, false, 0, true,
                5.0D, true, true, true, true);
        final Harness harness = harness(TraversalKind.WARP, options);

        harness.reachSource();
        harness.controller().tick();
        harness.currentSession().set(false);
        harness.scheduler().runScheduled();

        verify(harness.player(), never()).teleport(any(Location.class));
    }

    private Harness harness(final TraversalKind traversalKind, final ResolveAutoTeleportOptions options) {
        final Graph graph = new Graph(1, "route");
        final UUID sourceId = UUID.randomUUID();
        final UUID targetId = UUID.randomUUID();
        graph.addNode(new Node(sourceId, 0.0D, 64.0D, 0.0D, WORLD_ID));
        graph.addNode(new Node(targetId, 10.0D, 64.0D, 0.0D, WORLD_ID));
        graph.addDirectedEdge(sourceId, targetId, 1.0D, traversalKind == TraversalKind.NORMAL
                ? java.util.Set.of(EdgeFlag.DIRECTED)
                : java.util.Set.of(EdgeFlag.DIRECTED, EdgeFlag.TELEPORT));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        final NodeRef sourceRef = new NodeRef(1, sourceId);
        final NodeRef targetRef = new NodeRef(1, targetId);
        final PathResult path = new PathResult(List.of(sourceRef, targetRef),
                List.of(new PathSegment(sourceRef, targetRef, traversalKind, null, "warp")));
        final World world = mock(World.class);
        when(world.getUID()).thenReturn(WORLD_ID);
        final Player player = mock(Player.class);
        final FakeScheduler scheduler = new FakeScheduler();
        final AtomicLong tick = new AtomicLong();
        final AtomicBoolean currentSession = new AtomicBoolean(true);
        final LocationSource locationSource = new LocationSource(new Location(world, 0.5D, 64.5D, 0.5D));
        when(player.getLocation()).thenAnswer(invocation -> locationSource.locationValue());
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(new GraphNetworkVisualSource(network),
                path, locationSource::locationValue, new GuidedPathOptions(2, 5.0D, 0));
        final ResolveAutoTeleportController controller = new ResolveAutoTeleportController(network, path, source,
                options, () -> player, scheduler, tick::get, currentSession::get, null);
        return new Harness(player, world, source, controller, scheduler, locationSource, tick, currentSession);
    }

    private MockedStatic<Bukkit> mockBukkitWorld(final World world) {
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld(WORLD_ID)).thenReturn(world);
        return bukkit;
    }

    private record Harness(Player player, World world, GuidedPathVisualGraphSource source,
                           ResolveAutoTeleportController controller, FakeScheduler scheduler,
                           LocationSource locationSource, AtomicLong tick, AtomicBoolean currentSession) {

        private void reachSource() {
            source.snapshot();
        }

        private void movePlayer(final Location location) {
            locationSource.locationValue(location);
        }

        private void advanceTicks(final long ticks) {
            tick.addAndGet(ticks);
        }
    }

    private static final class LocationSource {

        private Location currentLocation;

        private LocationSource(final Location location) {
            this.currentLocation = location;
        }

        private Location locationValue() {
            return currentLocation;
        }

        private void locationValue(final Location location) {
            this.currentLocation = location;
        }
    }

    private static final class FakeScheduler implements ResolveAutoTeleportController.DelayScheduler {

        private final List<ScheduledAction> scheduled = new ArrayList<>();

        private long lastDelayTicks;

        private ScheduledAction lastAction;

        @Override
        public ResolveAutoTeleportController.ScheduledHandle schedule(final long delayTicks, final Runnable action) {
            lastDelayTicks = delayTicks;
            lastAction = new ScheduledAction(action);
            scheduled.add(lastAction);
            return lastAction::cancel;
        }

        private long lastScheduledDelayTicks() {
            return lastDelayTicks;
        }

        private boolean lastHandleCancelled() {
            return lastAction != null && lastAction.isCancelled();
        }

        private int scheduledCount() {
            return scheduled.size();
        }

        private void runScheduled() {
            if (!scheduled.isEmpty()) {
                scheduled.getLast().run();
            }
        }
    }

    private static final class ScheduledAction implements Runnable {

        private final Runnable action;

        private boolean cancelled;

        private ScheduledAction(final Runnable action) {
            this.action = action;
            this.cancelled = false;
        }

        @Override
        public void run() {
            if (!cancelled) {
                action.run();
            }
        }

        private void cancel() {
            cancelled = true;
        }

        private boolean isCancelled() {
            return cancelled;
        }
    }
}
