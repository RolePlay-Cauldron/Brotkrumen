package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PathFinderTest {

    @Test
    void returnsExpectedPathFromSelectedAlgorithm() {
        final PathAlgorithm algo = mock(PathAlgorithm.class);
        final SearchRegistry registry = mock(SearchRegistry.class);

        final Graph graph = new Graph();
        graph.addNode(new Node(1, 0, 0, 0));
        graph.addNode(new Node(2, 0, 0, 0));
        graph.addDirectedEdge(1, 2, 1.0);

        final TeleportRules rules = TeleportRules.disableTeleports();
        when(registry.select(graph, rules)).thenReturn(algo);

        final List<Node> expected = List.of(graph.getNodeById(1), graph.getNodeById(2));
        when(algo.findPath(graph, 1, 2, null, rules)).thenReturn(expected);

        final PathFinder finderWithRegistry = new PathFinder(registry);
        final PathFinder defaultFinder = new PathFinder();

        final List<Node> resultOne = finderWithRegistry.findPath(graph, 1, 2, null, rules);
        final List<Node> resultTwo = defaultFinder.findPath(graph, 1, 2, null, rules);

        assertEquals(List.of(expected, expected), List.of(resultOne, resultTwo), "The results should be the same");
    }

    @Test
    void delegatesToRegistryAndAlgorithm() {
        final PathAlgorithm algo = mock(PathAlgorithm.class);
        final SearchRegistry registry = mock(SearchRegistry.class);

        final Graph graph = new Graph();
        graph.addNode(new Node(1, 0, 0, 0));
        graph.addNode(new Node(2, 0, 0, 0));
        graph.addDirectedEdge(1, 2, 1.0);

        final TeleportRules rules = TeleportRules.disableTeleports();
        when(registry.select(graph, rules)).thenReturn(algo);
        when(algo.findPath(graph, 1, 2, null, rules))
                .thenReturn(List.of(graph.getNodeById(1), graph.getNodeById(2)));

        new PathFinder(registry).findPath(graph, 1, 2, null, rules);

        final InOrder inOrder = inOrder(registry, algo);
        inOrder.verify(registry).select(graph, rules);
        inOrder.verify(algo).findPath(graph, 1, 2, null, rules);
    }
}
