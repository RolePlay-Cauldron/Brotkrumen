package brotkrumen.graph.search;

import brotkrumen.graph.Graph;
import brotkrumen.graph.TeleportRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SearchRegistryTest {

    @Test
    void testSelectsFirstSuitableAlgorithm() {
        SearchRegistry registry = new SearchRegistry();

        PathAlgorithm algo1 = mock(PathAlgorithm.class);
        PathAlgorithm algo2 = mock(PathAlgorithm.class);

        when(algo1.suitable(any(), any())).thenReturn(false);
        when(algo2.suitable(any(), any())).thenReturn(true);

        registry.register(algo1);
        registry.register(algo2);

        Graph graph = new Graph();
        graph.addNode(new brotkrumen.graph.Node(1, 0, 0, 0));

        TeleportRules rules = TeleportRules.disableTeleports();

        PathAlgorithm selected = registry.select(graph, rules);

        assertSame(algo2, selected);
    }

    @Test
    void testUnregisterRemovesAlgorithm() {
        SearchRegistry registry = new SearchRegistry();
        PathAlgorithm algo = mock(PathAlgorithm.class);

        registry.register(algo);
        registry.unregister(algo);

        assertEquals(0, registry.all().size());
    }
}
