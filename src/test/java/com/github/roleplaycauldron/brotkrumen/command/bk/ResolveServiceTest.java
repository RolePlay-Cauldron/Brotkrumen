package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResolveService}.
 */
class ResolveServiceTest {

    private static final UUID WORLD = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void clampsResolveRadiusToViewDistance() {
        assertEquals(10.0D, ResolveOptions.effectiveNearestNodeRadius(15.0D, 10.0D),
                "Radius should be clamped to view distance");
        assertEquals(8.0D, ResolveOptions.effectiveNearestNodeRadius(8.0D, 10.0D),
                "Radius below view distance should be used as-is");
    }

    @Test
    void resolvesGraphByNameBeforeId() {
        final Graph graph = new Graph(12, "12");
        final GraphService graphService = mock(GraphService.class);
        when(graphService.getGraphByName("12")).thenReturn(Optional.of(graph));

        final Optional<Graph> resolved = new ResolveService(graphService).resolveGraph("12");

        assertSame(graph, resolved.orElseThrow(), "Graph name should win over numeric id parsing");
        verify(graphService, never()).getGraphById(12);
    }

    @Test
    void findsNearestNodeWithinRadius() {
        final Graph graph = new Graph(1, "sternchen");
        final Node near = graph.addNode(new Node(UUID.randomUUID(), 1.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(UUID.randomUUID(), 5.0D, 0.0D, 0.0D, WORLD));
        final ResolveService service = new ResolveService(mock(GraphService.class));

        final Optional<Node> nearest = service.nearestNode(graph, new ResolveLocation(WORLD, 0.0D, 0.0D, 0.0D),
                2.0D);

        assertEquals(near, nearest.orElseThrow(), "Nearest node inside radius should be selected");
    }

    @Test
    void rejectsNearestNodeOutsideRadius() {
        final Graph graph = new Graph(1, "sternchen");
        graph.addNode(new Node(UUID.randomUUID(), 3.0D, 0.0D, 0.0D, WORLD));
        final ResolveService service = new ResolveService(mock(GraphService.class));

        final Optional<Node> nearest = service.nearestNode(graph, new ResolveLocation(WORLD, 0.0D, 0.0D, 0.0D),
                2.0D);

        assertTrue(nearest.isEmpty(), "No node should be selected outside radius");
    }

    @Test
    void resolvesNodeTargetsOnlyWhenAllAreInOneGraph() {
        final Graph graph = new Graph(1, "sternchen");
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        graph.addNode(new Node(first, 0.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(second, 1.0D, 0.0D, 0.0D, WORLD));
        final GraphService graphService = mock(GraphService.class);
        when(graphService.getAllGraphs()).thenReturn(Set.of(graph));

        final ResolveService.NodeTargetResolution result = new ResolveService(graphService)
                .resolveNodeTargets(Set.of(first, second));

        assertTrue(result.success(), "Nodes in the same graph should resolve");
        assertSame(graph, result.graph(), "Containing graph should be returned");
    }

    @Test
    void rejectsNodeTargetsAcrossGraphs() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final Graph firstGraph = new Graph(1, "first");
        firstGraph.addNode(new Node(first, 0.0D, 0.0D, 0.0D, WORLD));
        final Graph secondGraph = new Graph(2, "second");
        secondGraph.addNode(new Node(second, 1.0D, 0.0D, 0.0D, WORLD));
        final GraphService graphService = mock(GraphService.class);
        when(graphService.getAllGraphs()).thenReturn(Set.of(firstGraph, secondGraph));

        final ResolveService.NodeTargetResolution result = new ResolveService(graphService)
                .resolveNodeTargets(Set.of(first, second));

        assertFalse(result.success(), "Nodes across graphs should be rejected");
    }

    @Test
    void findsGraphLocalPathToBestGoal() {
        final Graph graph = new Graph(1, "sternchen");
        final UUID start = UUID.randomUUID();
        final UUID middle = UUID.randomUUID();
        final UUID goal = UUID.randomUUID();
        graph.addNode(new Node(start, 0.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(middle, 1.0D, 0.0D, 0.0D, WORLD));
        graph.addNode(new Node(goal, 2.0D, 0.0D, 0.0D, WORLD));
        graph.addEdge(start, middle, 1.0D, Set.of(EdgeFlag.UNDIRECTED));
        graph.addEdge(middle, goal, 1.0D, Set.of(EdgeFlag.UNDIRECTED));

        final PathResult result = new ResolveService(mock(GraphService.class)).findPath(graph, start, Set.of(goal));

        assertFalse(result.nodes().isEmpty(), "Path should be found");
        assertEquals(goal, result.nodes().getLast().nodeId(), "Path should end at requested goal");
    }
}
