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

class DijkstraAlgorithmTest {

    private Graph graph;

    private DijkstraAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        graph = new Graph();

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

        graph.addUndirectedEdge(6, 7, 1.0, Set.of(EdgeFlag.TELEPORT));

        algorithm = new DijkstraAlgorithm();
    }

    @Test
    void testDijkstraSuitability() {
        final Warp spawn = new Warp("Spawn", 1, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        assertTrue(algorithm.suitable(graph, rules), "The algorithm should be suitable, but it isn't");
    }

    @Test
    void testDijkstraPathFinderWithLocalTeleport() {
        final Warp spawn = new Warp("Spawn", 1, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        final List<Node> pathNodes = algorithm.findPath(graph, 1, 7, null, rules);

        assertNotNull(pathNodes, "The path should not be null");

        final List<Integer> pathIds = pathNodes.stream().map(Node::id).collect(Collectors.toList());
        final List<Integer> expected = List.of(1, 6, 7);
        assertIterableEquals(expected, pathIds, "The node ids of the paths are not matching");
    }

    @Test
    void testDijkstraPathFinderWithGlobalTeleport() {
        final Warp spawn = new Warp("Spawn", 1, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        final List<Node> pathNodes = algorithm.findPath(graph, 3, 1, null, rules);

        assertNotNull(pathNodes, "The path should not be null");

        final List<Integer> pathIds = pathNodes.stream().map(Node::id).collect(Collectors.toList());
        final List<Integer> expected = List.of(3, 1);
        assertIterableEquals(expected, pathIds, "The node ids of the paths are not matching");
    }

    @Test
    void testBlockedPathWithoutGlobalTeleport() {
        final Warp spawn = new Warp("Spawn", 1, 1.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        final Predicate<Edge> filterWithoutGlobalTeleport =
                edge -> !edge.flags().contains(EdgeFlag.TELEPORT_GLOBAL) && !edge.flags().contains(EdgeFlag.BLOCKED);

        final List<Node> pathNodes = algorithm.findPath(graph, 3, 1, filterWithoutGlobalTeleport, rules);

        assertNotNull(pathNodes, "The path should not be null");

        final List<Integer> pathIds = pathNodes.stream().map(Node::id).collect(Collectors.toList());
        final List<Integer> expected = List.of(3, 7, 6, 1);
        assertIterableEquals(expected, pathIds, "The node ids of the paths are not matching");
    }
}
