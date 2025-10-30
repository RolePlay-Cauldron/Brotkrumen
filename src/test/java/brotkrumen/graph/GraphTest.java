package brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTest {

    @Test
    void graphCreateNodeTest() {
        Graph graph = new Graph();
        Node nodeOne = new Node(1, 2, 3, 4);
        Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);

        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.getNodes().contains(nodeOne));
        assertNotNull(graph.getNodeById(1));
    }

    @Test
    void graphCreateDirectedEdgeTest() {
        Graph graph = new Graph();
        Node nodeOne = new Node(1, 2, 3, 4);
        Node nodeTwo = new Node(2, 3, 4, 5);
        Node nodeThree = new Node(3, 5, 7, 8);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addNode(nodeThree);

        List<Edge> edges = graph.addUndirectedEdge(nodeOne.getId(), nodeTwo.getId(), 1.0);
        graph.addUndirectedEdge(nodeTwo.getId(), nodeThree.getId(), 1.0, EnumSet.of(EdgeFlag.ONE_WAY));

        assertNotNull(graph.getEdgeById(1));
    }

    @Test
    void graphCreateUndirectedEdgeTest() {
        Graph graph = new Graph();
        Node nodeOne = new Node(1, 2, 3, 4);
        Node nodeTwo = new Node(2, 3, 4, 5);
        Node nodeThree = new Node(3, 5, 7, 8);
        graph.addNode(nodeOne);
        graph.addNode(nodeTwo);
        graph.addNode(nodeThree);

        graph.addDirectedEdge(nodeOne.getId(), nodeTwo.getId(), 1.0);
        graph.addDirectedEdge(nodeTwo.getId(), nodeThree.getId(), 1.0, EnumSet.of(EdgeFlag.ONE_WAY));

        assertEquals(1, graph.neighbors(2).size());
    }

    @Test
    void graphCreateUndirectedEdgeExceptionTest() {
        Graph graph = new Graph();
        Node nodeOne = new Node(1, 2, 3, 4);
        Node nodeTwo = new Node(2, 3, 4, 5);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addUndirectedEdge(nodeOne.getId(), nodeTwo.getId(), 1.0));
    }

    @Test
    void graphExceptionTest() {
        Graph graph = new Graph();
        Node nodeOne = new Node(1, 2, 3, 4);
        graph.addNode(nodeOne);

        assertThrows(IllegalArgumentException.class, () -> graph.addNode(nodeOne));
    }
}
