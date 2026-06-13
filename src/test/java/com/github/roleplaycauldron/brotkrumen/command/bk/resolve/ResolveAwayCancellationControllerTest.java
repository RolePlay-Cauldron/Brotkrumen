package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.SingleGraphVisualSource;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResolveAwayCancellationController}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResolveAwayCancellationControllerTest {

    private static Location nearEdgeLocation() {
        return new Location(null, 5.5D, 0.5D, 0.5D);
    }

    private static Location farLocation() {
        return new Location(null, 5.5D, 20.0D, 0.5D);
    }

    @Test
    void warnsOnceAndCancelsAfterGracePeriod() {
        final Harness harness = harness(new ResolveAwayCancellationOptions(true, 1.0D, true, 1));
        harness.location().locationValue = farLocation();

        harness.controller().tick();
        harness.tick().set(19L);
        harness.controller().tick();
        harness.tick().set(20L);
        harness.controller().tick();

        verify(harness.localization()).getPrefixedMessage("commands.bk.resolve.status.cancelWarning");
        verify(harness.localization()).getPrefixedMessage("commands.bk.resolve.status.cancelledAway");
        verify(harness.player(), times(2)).sendMessage(any(Component.class));
        assertEquals(1, harness.cancelCount().get(), "Session should cancel once after grace expires");
    }

    @Test
    void returningDuringGraceKeepsSessionActiveAndAllowsFutureWarning() {
        final Harness harness = harness(new ResolveAwayCancellationOptions(true, 1.0D, true, 1));
        harness.location().locationValue = farLocation();
        harness.controller().tick();
        harness.tick().set(10L);
        harness.location().locationValue = nearEdgeLocation();
        harness.controller().tick();
        harness.tick().set(20L);
        harness.location().locationValue = farLocation();
        harness.controller().tick();

        verify(harness.localization(), times(2)).getPrefixedMessage("commands.bk.resolve.status.cancelWarning");
        verify(harness.localization(), never()).getPrefixedMessage("commands.bk.resolve.status.cancelledAway");
        assertEquals(0, harness.cancelCount().get(), "Session should remain active after re-entry");
    }

    @Test
    void warningCanBeDisabledWhileCancellationRemainsEnabled() {
        final Harness harness = harness(new ResolveAwayCancellationOptions(true, 1.0D, false, 0));
        harness.location().locationValue = farLocation();

        harness.controller().tick();

        verify(harness.localization(), never()).getPrefixedMessage("commands.bk.resolve.status.cancelWarning");
        verify(harness.localization()).getPrefixedMessage("commands.bk.resolve.status.cancelledAway");
        assertEquals(1, harness.cancelCount().get(), "Zero grace should cancel immediately");
    }

    @Test
    void disabledAwayCancellationDoesNothing() {
        final Harness harness = harness(new ResolveAwayCancellationOptions(false, 1.0D, true, 0));
        harness.location().locationValue = farLocation();

        harness.controller().tick();

        verify(harness.localization(), never()).getPrefixedMessage(any(String.class));
        assertEquals(0, harness.cancelCount().get(), "Disabled away-cancellation should not cancel");
    }

    @Test
    void completedSourceIsNotCancelled() {
        final Harness harness = harness(new ResolveAwayCancellationOptions(true, 1.0D, true, 0));
        harness.location().locationValue = new Location(null, 10.5D, 0.5D, 0.5D);
        harness.source().snapshot();
        harness.location().locationValue = farLocation();

        harness.controller().tick();

        verify(harness.localization(), never()).getPrefixedMessage(eq("commands.bk.resolve.status.cancelledAway"));
        assertEquals(0, harness.cancelCount().get(), "Completed guidance should not be away-cancelled");
    }

    private Harness harness(final ResolveAwayCancellationOptions options) {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final NodeRef firstRef = new NodeRef(1, first);
        final NodeRef secondRef = new NodeRef(1, second);
        final Graph graph = new Graph(1, "Resolve Away");
        graph.addNode(new Node(first, 0, 0, 0, null));
        graph.addNode(new Node(second, 10, 0, 0, null));
        graph.addDirectedEdge(first, second, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final LocationSource location = new LocationSource(nearEdgeLocation());
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(new SingleGraphVisualSource(graph),
                new PathResult(List.of(firstRef, secondRef), List.of()), location::currentLocation,
                new GuidedPathOptions(2, 1.0D, 0));
        final Player player = mock(Player.class);
        final Localization localization = mock(Localization.class);
        when(localization.getPrefixedMessage(any(String.class))).thenReturn(Component.text("message"));
        final AtomicLong tick = new AtomicLong();
        final AtomicInteger cancelCount = new AtomicInteger();
        final ResolveAwayCancellationController controller = new ResolveAwayCancellationController(source, options,
                () -> player, tick::get, () -> true, cancelCount::incrementAndGet, localization);
        return new Harness(controller, source, location, player, localization, tick, cancelCount);
    }

    private static final class LocationSource {

        private Location locationValue;

        private LocationSource(final Location currentLocation) {
            this.locationValue = currentLocation;
        }

        private Location currentLocation() {
            return locationValue;
        }
    }

    private record Harness(ResolveAwayCancellationController controller, GuidedPathVisualGraphSource source,
                           LocationSource location, Player player, Localization localization, AtomicLong tick,
                           AtomicInteger cancelCount) {
    }
}
