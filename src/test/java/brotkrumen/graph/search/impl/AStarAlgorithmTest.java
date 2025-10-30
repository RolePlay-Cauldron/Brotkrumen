package brotkrumen.graph.search.impl;

import brotkrumen.graph.Edge;
import brotkrumen.graph.EdgeFlag;
import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;
import brotkrumen.graph.Warp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class AStarAlgorithmTest {
    private Graph graph;

    private AStarAlgorithm algorithm;

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

        graph.addUndirectedEdge(1, 2, 40, EnumSet.of(EdgeFlag.BLOCKED));
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
        TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        Warp spawn = new Warp("Spawn", 1, 1.0, true);
        TeleportRules tpAllowed = new TeleportRules(true, true, List.of(spawn));
        TeleportRules tpLocalAllowed = new TeleportRules(false, true, List.of());

        assertTrue(algorithm.suitable(graph, tpNotAllowed));
        assertFalse(algorithm.suitable(graph, tpAllowed));
        assertFalse(algorithm.suitable(graph, tpLocalAllowed));
        assertFalse(algorithm.suitable(graph, null));
    }

    @Test
    void testAStarPathFinding() {
        TeleportRules tpNotAllowed = TeleportRules.disableTeleports();
        List<Node> pathNodes = algorithm.findPath(graph, 1, 7, null, tpNotAllowed);

        assertNotNull(pathNodes);
        assertFalse(pathNodes.isEmpty());
        assertEquals(1, pathNodes.getFirst().getId());
        assertEquals(7, pathNodes.getLast().getId());

        List<Integer> pathIds = pathNodes.stream().map(Node::getId).collect(Collectors.toList());
        List<Integer> expected = List.of(1, 2, 3, 7);

        assertIterableEquals(expected, pathIds);
    }

    @Test
    void testAStarBlockedPath() {
        TeleportRules tpNotAllowed = TeleportRules.disableTeleports();

        Predicate<Edge> filterWithoutBlocked = edge -> !edge.hasFlag(EdgeFlag.BLOCKED);
        List<Node> pathNodes = algorithm.findPath(graph, 1, 7, filterWithoutBlocked, tpNotAllowed);

        assertNotNull(pathNodes);
        assertFalse(pathNodes.isEmpty());
        assertEquals(1, pathNodes.getFirst().getId());
        assertEquals(7, pathNodes.getLast().getId());

        List<Integer> pathIds = pathNodes.stream().map(Node::getId).collect(Collectors.toList());
        List<Integer> expected = List.of(1, 5, 4, 7);

        assertIterableEquals(expected, pathIds);
    }
}
