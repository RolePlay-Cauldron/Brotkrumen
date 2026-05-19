package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the {@link PathFinder}.
 */
class PathFinderTest {
    private final UUID uuidOne = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

    private final UUID uuidTwo = UUID.fromString("18a6d815-2c26-4fde-8179-e74baca4bb4e");

    @Test
    void returnsExpectedPathFromSelectedAlgorithm() {
        final PathAlgorithm algo = mock(PathAlgorithm.class);
        final SearchRegistry registry = mock(SearchRegistry.class);

        final Graph graph = new Graph("Test");
        graph.addNode(new Node(uuidOne, 0, 0, 0, null));
        graph.addNode(new Node(uuidTwo, 0, 0, 0, null));
        graph.addDirectedEdge(uuidOne, uuidTwo, 1.0);

        final TeleportRules rules = TeleportRules.disableTeleports();
        when(registry.select(graph, rules)).thenReturn(algo);

        when(algo.findPathResult(graph, uuidOne, Set.of(uuidTwo), null, rules)).thenReturn(
                new PathResult(expectedRefs(graph), List.of()));

        final PathFinder finderWithRegistry = new PathFinder(registry);
        final PathFinder defaultFinder = new PathFinder();

        final List<NodeRef> resultOne = finderWithRegistry.findPathResult(graph, uuidOne, uuidTwo, null, rules).nodes();
        final List<NodeRef> resultTwo = defaultFinder.findPathResult(graph, uuidOne, uuidTwo, null, rules).nodes();

        final List<NodeRef> expectedRefs = expectedRefs(graph);
        assertEquals(List.of(expectedRefs, expectedRefs), List.of(resultOne, resultTwo), "The results should be the same");
    }

    @Test
    void delegatesToRegistryAndAlgorithm() {
        final PathAlgorithm algo = mock(PathAlgorithm.class);
        final SearchRegistry registry = mock(SearchRegistry.class);

        final Graph graph = new Graph("Test");
        graph.addNode(new Node(uuidOne, 0, 0, 0, null));
        graph.addNode(new Node(uuidTwo, 0, 0, 0, null));
        graph.addDirectedEdge(uuidOne, uuidTwo, 1.0);

        final TeleportRules rules = TeleportRules.disableTeleports();
        when(registry.select(graph, rules)).thenReturn(algo);
        when(algo.findPathResult(graph, uuidOne, Set.of(uuidTwo), null, rules))
                .thenReturn(new PathResult(expectedRefs(graph), List.of()));

        new PathFinder(registry).findPathResult(graph, uuidOne, uuidTwo, null, rules);

        final InOrder inOrder = inOrder(registry, algo);
        inOrder.verify(registry).select(graph, rules);
        inOrder.verify(algo).findPathResult(graph, uuidOne, Set.of(uuidTwo), null, rules);
    }

    @Test
    void findsPathAcrossTwoGraphs() {
        final Graph graphOne = new Graph(1, "One");
        final UUID g1A = UUID.fromString("4d15ef07-8da5-4659-a824-70f477758456");
        final UUID g1B = UUID.fromString("34afc870-bf72-4540-b7ab-49bc776f16c6");
        graphOne.addNode(new Node(g1A, 0, 64, 0, null));
        graphOne.addNode(new Node(g1B, 1, 64, 0, null));
        graphOne.addDirectedEdge(g1A, g1B, 1.0, Set.of(EdgeFlag.DIRECTED));

        final Graph graphTwo = new Graph(2, "Two");
        final UUID g2A = UUID.fromString("36f95b5a-8b05-4689-bf32-adf2f2a2753f");
        final UUID g2B = UUID.fromString("8c6fe635-6fdf-4f43-94d6-bc4e43d44cb9");
        graphTwo.addNode(new Node(g2A, 10, 64, 0, null));
        graphTwo.addNode(new Node(g2B, 11, 64, 0, null));
        graphTwo.addDirectedEdge(g2A, g2B, 1.0, Set.of(EdgeFlag.DIRECTED));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addInterGraphEdge(new InterGraphEdge(UUID.randomUUID(), new NodeRef(1, g1B), new NodeRef(2, g2A),
                2.0, Set.of(EdgeFlag.INTER_GRAPH, EdgeFlag.DIRECTED), true));

        final PathFinder pathFinder = new PathFinder();

        final List<NodeRef> path = pathFinder.findPathResult(network, new NodeRef(1, g1A), new NodeRef(2, g2B), null,
                TeleportRules.disableTeleports()).nodes();

        assertEquals(List.of(new NodeRef(1, g1A), new NodeRef(1, g1B), new NodeRef(2, g2A), new NodeRef(2, g2B)), path,
                "Path should cross the inter-graph edge");
    }

    @Test
    void interGraphTeleportObeysIndependentRuleSwitch() {
        final Graph graphOne = new Graph(1, "One");
        final UUID source = UUID.randomUUID();
        graphOne.addNode(new Node(source, 0, 0, 0, null));
        final Graph graphTwo = new Graph(2, "Two");
        final UUID target = UUID.randomUUID();
        graphTwo.addNode(new Node(target, 10, 0, 0, null));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        network.addDirectedInterGraphEdge(new NodeRef(1, source), new NodeRef(2, target), 1.0D,
                Set.of(EdgeFlag.TELEPORT, EdgeFlag.INTER_GRAPH));

        final PathFinder pathFinder = new PathFinder();

        assertEquals(List.of(new NodeRef(1, source), new NodeRef(2, target)),
                pathFinder.findPathResult(network, new NodeRef(1, source), new NodeRef(2, target), null,
                        new TeleportRules(false, true, false, List.of())).nodes(),
                "Enabled intergraph teleport should be traversable");
        assertTrue(pathFinder.findPathResult(network, new NodeRef(1, source), new NodeRef(2, target), null,
                        new TeleportRules(false, false, false, List.of())).nodes().isEmpty(),
                "Disabled intergraph teleport should be excluded from search input");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void warpRoutesAreGloballyCallableAndDisappearWhenTargetIsRemoved() {
        final Graph graphOne = new Graph(1, "One");
        final UUID source = UUID.randomUUID();
        graphOne.addNode(new Node(source, 0, 0, 0, null));
        final Graph graphTwo = new Graph(2, "Two");
        final UUID target = UUID.randomUUID();
        graphTwo.addNode(new Node(target, 10, 0, 0, null, Set.of(NodeFlag.WARP)));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        final NodeRef sourceRef = new NodeRef(1, source);
        final NodeRef targetRef = new NodeRef(2, target);
        final TeleportRules enabledRules = new TeleportRules(false, false, true, List.of(new Warp("target", target, 0.0D, true, false)));
        final PathFinder pathFinder = new PathFinder();

        assertEquals(List.of(sourceRef, targetRef), pathFinder.findPathResult(network, sourceRef, targetRef, null, enabledRules).nodes(),
                "Warp target should be reachable through globally callable warp traversal");
        assertEquals(0, network.getInterGraphEdges().size(), "Derived warp routes should not be persisted as edges");

        graphTwo.removeNode(graphTwo.getNodeById(target));

        assertTrue(pathFinder.findPathResult(network, sourceRef, targetRef, null, enabledRules).nodes().isEmpty(),
                "Removing the warp target node should remove routes from later search input");
    }

    @Test
    void networkPathResultKeepsIntergraphAndWarpSegmentMetadata() {
        final Graph graphOne = new Graph(1, "One");
        final UUID source = UUID.randomUUID();
        graphOne.addNode(new Node(source, 0, 0, 0, null));
        final Graph graphTwo = new Graph(2, "Two");
        final UUID target = UUID.randomUUID();
        graphTwo.addNode(new Node(target, 10, 0, 0, null, Set.of(NodeFlag.WARP)));
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graphOne);
        network.addGraph(graphTwo);
        final NodeRef sourceRef = new NodeRef(1, source);
        final NodeRef targetRef = new NodeRef(2, target);
        final TeleportRules rules = new TeleportRules(false, false, true, List.of(new Warp("target", target, 1.0D, true, false)));

        final PathResult result = new PathFinder().findPathResult(network, sourceRef, targetRef, null, rules);

        assertEquals(List.of(sourceRef, targetRef), result.nodes(), "Network result should map unified IDs back to node refs");
        assertEquals(TraversalKind.WARP, result.segments().getFirst().traversalKind(),
                "Network warp traversal should keep segment metadata");
    }

    private List<NodeRef> expectedRefs(final Graph graph) {
        return List.of(new NodeRef(graph.getGraphId(), uuidOne), new NodeRef(graph.getGraphId(), uuidTwo));
    }
}
