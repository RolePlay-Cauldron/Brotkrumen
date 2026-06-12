package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.util.DirectSimpleScheduler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PathSearchServiceImplTest {

    private final DirectSimpleScheduler scheduler = new DirectSimpleScheduler();

    @Test
    void graphPathSearchCompletesThroughFuture() {
        final PathFinder pathFinder = mock(PathFinder.class);
        final Graph graph = new Graph(3, "Route");
        final UUID start = UUID.randomUUID();
        final UUID goal = UUID.randomUUID();
        graph.addNode(new Node(start, 0, 0, 0, null));
        graph.addNode(new Node(goal, 1, 0, 0, null));
        final TeleportRules rules = TeleportRules.disableTeleports();
        final PathResult expected = new PathResult(List.of(new NodeRef(3, start), new NodeRef(3, goal)), List.of());
        when(pathFinder.findPathResult(graph, start, goal, null, rules)).thenReturn(expected);
        final PathSearchServiceImpl service = new PathSearchServiceImpl(pathFinder, scheduler);

        assertEquals(expected, service.findPath(graph, start, goal, null, rules).join(),
                "Future should expose path result");
    }
}
