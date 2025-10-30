package brotkrumen.graph.search;

import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PathFinderTest {

    @Test
    void testPathfinderUsesSelectedAlgorithm() {
        PathAlgorithm algo = mock(PathAlgorithm.class);
        SearchRegistry registry = mock(SearchRegistry.class);


        Graph graph = new Graph();
        graph.addNode(new Node(1, 0, 0, 0));
        graph.addNode(new Node(2, 0, 0, 0));
        graph.addDirectedEdge(1, 2, 1.0);

        TeleportRules rules = TeleportRules.disableTeleports();

        when(registry.select(graph, rules)).thenReturn(algo);

        List<Node> expected = List.of(graph.getNodeById(1), graph.getNodeById(2));
        when(algo.findPath(graph, 1, 2, null, rules)).thenReturn(expected);

        PathFinder finderOne = new PathFinder(registry);
        PathFinder finderTwo = new PathFinder();
        List<Node> resultOne = finderOne.findPath(graph, 1, 2, null, rules);
        List<Node> resultTwo = finderTwo.findPath(graph, 1, 2, null, rules);

        assertEquals(expected, resultOne);
        assertEquals(expected, resultTwo);
        verify(registry).select(graph, rules);
        verify(algo).findPath(graph, 1, 2, null, rules);
    }
}
