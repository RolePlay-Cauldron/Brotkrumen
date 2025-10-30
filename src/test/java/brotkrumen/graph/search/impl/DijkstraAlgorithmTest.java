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

public class DijkstraAlgorithmTest {

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

        graph.addUndirectedEdge(1, 2, 40, EnumSet.of(EdgeFlag.BLOCKED));
        graph.addUndirectedEdge(1, 6, 40);
        graph.addDirectedEdge(1, 5, 40);
        graph.addUndirectedEdge(2, 3, 40);
        graph.addUndirectedEdge(3, 7, 40);
        graph.addUndirectedEdge(7, 4, 40);
        graph.addUndirectedEdge(4, 5, 40);
        graph.addUndirectedEdge(5, 6, 40);

        graph.addUndirectedEdge(6, 7, 1.0, EnumSet.of(EdgeFlag.TELEPORT));

        algorithm = new DijkstraAlgorithm();
    }

    @Test
    void testDijkstraSuitability() {
        Warp spawn = new Warp("Spawn", 1, 1.0, true);
        TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        assertTrue(algorithm.suitable(graph, rules));
    }

    @Test
    void testDijkstraPathFinderWithLocalTeleport() {
        Warp spawn = new Warp("Spawn", 1, 1.0, true);
        TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        List<Node> pathNodes = algorithm.findPath(graph, 1, 7, null, rules);

        assertNotNull(pathNodes);
        assertFalse(pathNodes.isEmpty());
        assertEquals(1, pathNodes.getFirst().getId());
        assertEquals(7, pathNodes.getLast().getId());

        List<Integer> pathIds = pathNodes.stream().map(Node::getId).collect(Collectors.toList());
        List<Integer> expected = List.of(1, 6, 7);

        assertIterableEquals(expected, pathIds);
    }

    @Test
    void testDijkstraPathFinderWithGlobalTeleport() {
        Warp spawn = new Warp("Spawn", 1, 1.0, true);
        TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        List<Node> pathNodes = algorithm.findPath(graph, 3, 1, null, rules);

        assertNotNull(pathNodes);
        assertFalse(pathNodes.isEmpty());
        assertEquals(3, pathNodes.getFirst().getId());
        assertEquals(1, pathNodes.getLast().getId());

        List<Integer> pathIds = pathNodes.stream().map(Node::getId).collect(Collectors.toList());
        List<Integer> expected = List.of(3, 1);

        assertIterableEquals(expected, pathIds);
    }

    @Test
    void testAStarBlockedPath() {
        Warp spawn = new Warp("Spawn", 1, 1.0, true);
        TeleportRules rules = new TeleportRules(true, true, List.of(spawn));
        Predicate<Edge> filterWithoutGlobalTeleport = edge -> !edge.hasFlag(EdgeFlag.TELEPORT_GLOBAL) && !edge.hasFlag(EdgeFlag.BLOCKED);
        List<Node> pathNodes = algorithm.findPath(graph, 3, 1, filterWithoutGlobalTeleport, rules);

        assertNotNull(pathNodes);
        assertFalse(pathNodes.isEmpty());
        assertEquals(3, pathNodes.getFirst().getId());
        assertEquals(1, pathNodes.getLast().getId());

        List<Integer> pathIds = pathNodes.stream().map(Node::getId).collect(Collectors.toList());
        List<Integer> expected = List.of(3, 7, 6, 1);

        assertIterableEquals(expected, pathIds);
    }
}
