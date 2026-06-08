package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResolveWarpStartSelector}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResolveWarpStartSelectorTest {

    private static final UUID WORLD = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void selectsCheapestWarpRouteToNodeTarget() {
        final UUID expensiveStart = UUID.randomUUID();
        final UUID cheapStart = UUID.randomUUID();
        final UUID target = UUID.randomUUID();
        final Graph graph = new Graph(1, "route");
        graph.addNode(new Node(expensiveStart, 0.0D, 64.0D, 0.0D, WORLD));
        graph.addNode(new Node(cheapStart, 10.0D, 64.0D, 0.0D, WORLD));
        graph.addNode(new Node(target, 20.0D, 64.0D, 0.0D, WORLD));
        graph.addDirectedEdge(expensiveStart, target, 20.0D, Set.of(EdgeFlag.DIRECTED));
        graph.addDirectedEdge(cheapStart, target, 2.0D, Set.of(EdgeFlag.DIRECTED));
        final GraphNetwork network = network(graph);
        final ResolveWarpStartSelector selector = selector(graph, network);
        final TeleportRules rules = new TeleportRules(false, false, true, List.of(
                new Warp("expensive", expensiveStart, 0.0D, true, false),
                new Warp("cheap", cheapStart, 0.0D, true, false)
        ));

        final ResolveWarpStartSelector.Candidate candidate = selector.selectNodes(
                        ResolveOptions.fromConfig(new org.bukkit.configuration.file.YamlConfiguration()),
                        location(), List.of(new NodeRef(1, target)), rules)
                .orElseThrow();

        assertNotEquals(new NodeRef(1, cheapStart), candidate.start(), "Fallback should start from a temporary node");
        assertEquals(new NodeRef(1, cheapStart), candidate.path().nodes().get(1),
                "Cheapest warp route should be selected");
        assertEquals(2.0D, candidate.cost(), "Candidate cost should use route segment cost");
    }

    @Test
    void selectsDirectWarpToNodeTarget() {
        final UUID target = UUID.randomUUID();
        final Graph graph = new Graph(1, "route");
        graph.addNode(new Node(target, 20.0D, 64.0D, 0.0D, WORLD));
        final ResolveWarpStartSelector selector = selector(graph, network(graph));
        final TeleportRules rules = new TeleportRules(false, false, true,
                List.of(new Warp("target", target, 3.0D, true, false)));

        final ResolveWarpStartSelector.Candidate candidate = selector.selectNodes(
                        ResolveOptions.fromConfig(new org.bukkit.configuration.file.YamlConfiguration()),
                        location(), List.of(new NodeRef(1, target)), rules)
                .orElseThrow();

        assertEquals(2, candidate.path().nodes().size(), "Direct warp should route from temp node to target");
        assertEquals(new NodeRef(1, target), candidate.path().nodes().getLast(), "Direct warp should end at target");
        assertEquals(TraversalKind.WARP, candidate.path().segments().getFirst().traversalKind(),
                "Direct route should preserve warp segment metadata");
        assertEquals(3.0D, candidate.cost(), "Candidate cost should include the warp cost");
    }

    @Test
    void ignoresWarpFallbackWhenDisabled() {
        final UUID start = UUID.randomUUID();
        final UUID target = UUID.randomUUID();
        final Graph graph = new Graph(1, "route");
        graph.addNode(new Node(start, 0.0D, 64.0D, 0.0D, WORLD));
        graph.addNode(new Node(target, 1.0D, 64.0D, 0.0D, WORLD));
        graph.addDirectedEdge(start, target, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final ResolveWarpStartSelector selector = selector(graph, network(graph));
        final TeleportRules rules = new TeleportRules(false, false, true,
                List.of(new Warp("start", start, 0.0D, true, false)));
        final org.bukkit.configuration.file.YamlConfiguration config =
                new org.bukkit.configuration.file.YamlConfiguration();
        config.set("commands.resolve.autoTeleport.startFromWarpWhenNoNearbyNode", false);

        assertTrue(selector.selectNodes(ResolveOptions.fromConfig(config), location(),
                        List.of(new NodeRef(1, target)), rules)
                .isEmpty(), "Disabled fallback should not select a warp");
    }

    @Test
    void ignoresWarpFallbackWhenWarpsAreNotAllowed() {
        final UUID start = UUID.randomUUID();
        final UUID target = UUID.randomUUID();
        final Graph graph = new Graph(1, "route");
        graph.addNode(new Node(start, 0.0D, 64.0D, 0.0D, WORLD));
        graph.addNode(new Node(target, 1.0D, 64.0D, 0.0D, WORLD));
        graph.addDirectedEdge(start, target, 1.0D, Set.of(EdgeFlag.DIRECTED));
        final ResolveWarpStartSelector selector = selector(graph, network(graph));
        final TeleportRules rules = new TeleportRules(false, false, false,
                List.of(new Warp("start", start, 0.0D, true, false)));

        assertTrue(selector.selectNodes(
                ResolveOptions.fromConfig(new org.bukkit.configuration.file.YamlConfiguration()), location(),
                List.of(new NodeRef(1, target)), rules).isEmpty(), "Teleport rules should control warp fallback");
    }

    @Test
    void ignoresWarpWithoutRouteToTarget() {
        final UUID start = UUID.randomUUID();
        final UUID target = UUID.randomUUID();
        final Graph graph = new Graph(1, "route");
        graph.addNode(new Node(start, 0.0D, 64.0D, 0.0D, WORLD));
        graph.addNode(new Node(target, 1.0D, 64.0D, 0.0D, WORLD));
        final ResolveWarpStartSelector selector = selector(graph, network(graph));
        final TeleportRules rules = new TeleportRules(false, false, true,
                List.of(new Warp("start", start, 0.0D, true, false)));

        assertTrue(selector.selectNodes(
                ResolveOptions.fromConfig(new org.bukkit.configuration.file.YamlConfiguration()), location(),
                List.of(new NodeRef(1, target)), rules).isEmpty(), "Unroutable warp should not be selected");
    }

    private ResolveLocation location() {
        return new ResolveLocation(WORLD, -10.0D, 64.0D, 0.0D);
    }

    private GraphNetwork network(final Graph graph) {
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        return network;
    }

    private ResolveWarpStartSelector selector(final Graph graph, final GraphNetwork network) {
        final GraphService graphService = mock(GraphService.class);
        final GraphNetworkService graphNetworkService = mock(GraphNetworkService.class);
        when(graphService.getAllGraphs()).thenReturn(Set.of(graph));
        when(graphService.getGraphById(graph.getGraphId())).thenReturn(Optional.of(graph));
        when(graphNetworkService.loadGraphNetworks()).thenReturn(List.of(network));
        return new ResolveWarpStartSelector(graphService, new ResolveService(graphService, graphNetworkService));
    }
}
