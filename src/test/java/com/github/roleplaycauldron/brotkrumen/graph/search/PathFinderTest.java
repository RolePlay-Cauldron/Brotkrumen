package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
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

        final List<Node> expected = List.of(graph.getNodeById(uuidOne), graph.getNodeById(uuidTwo));
        when(algo.findPath(graph, uuidOne, Set.of(uuidTwo), null, rules)).thenReturn(expected);

        final PathFinder finderWithRegistry = new PathFinder(registry);
        final PathFinder defaultFinder = new PathFinder();

        final List<Node> resultOne = finderWithRegistry.findPath(graph, uuidOne, uuidTwo, null, rules);
        final List<Node> resultTwo = defaultFinder.findPath(graph, uuidOne, uuidTwo, null, rules);

        assertEquals(List.of(expected, expected), List.of(resultOne, resultTwo), "The results should be the same");
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
        when(algo.findPath(graph, uuidOne, Set.of(uuidTwo), null, rules))
                .thenReturn(List.of(graph.getNodeById(uuidOne), graph.getNodeById(uuidTwo)));

        new PathFinder(registry).findPath(graph, uuidOne, uuidTwo, null, rules);

        final InOrder inOrder = inOrder(registry, algo);
        inOrder.verify(registry).select(graph, rules);
        inOrder.verify(algo).findPath(graph, uuidOne, Set.of(uuidTwo), null, rules);
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

        final List<NodeRef> path = pathFinder.findPath(network, new NodeRef(1, g1A), new NodeRef(2, g2B), null,
                TeleportRules.disableTeleports());
        final List<Node> nodePath = pathFinder.findNodePath(network, new NodeRef(1, g1A), new NodeRef(2, g2B), null,
                TeleportRules.disableTeleports());

        assertEquals(List.of(new NodeRef(1, g1A), new NodeRef(1, g1B), new NodeRef(2, g2A), new NodeRef(2, g2B)), path,
                "Path should cross the inter-graph edge");
        assertEquals(List.of(g1A, g1B, g2A, g2B), nodePath.stream().map(Node::graphId).toList(),
                "Resolved node path should be usable by visualizers");
    }
}
