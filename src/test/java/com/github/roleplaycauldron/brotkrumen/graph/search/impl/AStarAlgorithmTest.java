package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the {@link AStarAlgorithm}.
 */
class AStarAlgorithmTest {

    private Graph graph;

    private AStarAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        graph = new Graph("Test");

        graph.addNode(new Node(1, 0, 0, 0));
        graph.addNode(new Node(2, 35, 15, 0));
        graph.addNode(new Node(3, 65, 5, 0));
        graph.addNode(new Node(4, 60, -20, 0));
        graph.addNode(new Node(5, 35, -5, 0));
        graph.addNode(new Node(6, 5, -25, 0));
        graph.addNode(new Node(7, 90, -5, 0));

        graph.addUndirectedEdge(1, 2, 40, Set.of(EdgeFlag.BLOCKED));
        graph.addUndirectedEdge(1, 6, 40);
        graph.addDirectedEdge(1, 5, 40);
        graph.addUndirectedEdge(2, 3, 40);
        graph.addUndirectedEdge(3, 7, 40);
        graph.addUndirectedEdge(7, 4, 40);
        graph.addUndirectedEdge(4, 5, 40);
        graph.addUndirectedEdge(5, 6, 40);

        algorithm = new AStarAlgorithm();
    }

    @Test
    void testSuitability() {
        final TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        final Warp spawn = new Warp("Spawn", 1, 1.0, true);
        final TeleportRules tpAllowed = new TeleportRules(true, true, List.of(spawn));

        assertTrue(algorithm.suitable(graph, tpNotAllowed), "Algorithm should be suitable");
        assertFalse(algorithm.suitable(graph, tpAllowed), "Algorithm should not be suitable with teleport allowed");
    }

    @Test
    void testAStarPathFinding() {
        final TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        final List<Node> pathNodes = algorithm.findPath(graph, 1, 7, null, tpNotAllowed);

        assertNotNull(pathNodes, "The path should not be null");

        final List<Integer> actual = pathNodes.stream().map(Node::graphId).collect(Collectors.toList());
        final List<Integer> expected = List.of(1, 2, 3, 7);

        assertIterableEquals(expected, actual, "The path should match expected");
    }

    @Test
    void testAStarBlockedPath() {
        final TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        final Predicate<Edge> filterWithoutBlocked = edge -> !edge.flags().contains(EdgeFlag.BLOCKED);

        final List<Node> pathNodes = algorithm.findPath(graph, 1, 7, filterWithoutBlocked, tpNotAllowed);

        assertNotNull(pathNodes, "The path should not be null");

        final List<Integer> actual = pathNodes.stream().map(Node::graphId).collect(Collectors.toList());
        final List<Integer> expected = List.of(1, 5, 4, 7);

        assertIterableEquals(expected, actual, "The path should match expected");
    }
}
