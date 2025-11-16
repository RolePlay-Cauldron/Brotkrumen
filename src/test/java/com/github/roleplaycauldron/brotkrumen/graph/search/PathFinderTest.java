package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
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
        graph.addNode(new Node(uuidOne, 0, 0, 0));
        graph.addNode(new Node(uuidTwo, 0, 0, 0));
        graph.addDirectedEdge(uuidOne, uuidTwo, 1.0);

        final TeleportRules rules = TeleportRules.disableTeleports();
        when(registry.select(graph, rules)).thenReturn(algo);

        final List<Node> expected = List.of(graph.getNodeById(uuidOne), graph.getNodeById(uuidTwo));
        when(algo.findPath(graph, uuidOne, uuidTwo, null, rules)).thenReturn(expected);

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
        graph.addNode(new Node(uuidOne, 0, 0, 0));
        graph.addNode(new Node(uuidTwo, 0, 0, 0));
        graph.addDirectedEdge(uuidOne, uuidTwo, 1.0);

        final TeleportRules rules = TeleportRules.disableTeleports();
        when(registry.select(graph, rules)).thenReturn(algo);
        when(algo.findPath(graph, uuidOne, uuidTwo, null, rules))
                .thenReturn(List.of(graph.getNodeById(uuidOne), graph.getNodeById(uuidTwo)));

        new PathFinder(registry).findPath(graph, uuidOne, uuidTwo, null, rules);

        final InOrder inOrder = inOrder(registry, algo);
        inOrder.verify(registry).select(graph, rules);
        inOrder.verify(algo).findPath(graph, uuidOne, uuidTwo, null, rules);
    }
}
