package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the {@link SearchRegistry}.
 */
class SearchRegistryTest {

    @Test
    void testSelectsFirstSuitableAlgorithm() {
        final SearchRegistry registry = new SearchRegistry();

        final PathAlgorithm algo1 = mock(PathAlgorithm.class);
        final PathAlgorithm algo2 = mock(PathAlgorithm.class);

        when(algo1.suitable(any(), any())).thenReturn(false);
        when(algo2.suitable(any(), any())).thenReturn(true);

        registry.register(algo1);
        registry.register(algo2);

        final Graph graph = new Graph("Test");
        graph.addNode(new Node(1, 0, 0, 0));

        final TeleportRules rules = TeleportRules.disableTeleports();

        final PathAlgorithm selected = registry.select(graph, rules);

        assertSame(algo2, selected, "The algorithm should be the same");
    }

    @Test
    void testUnregisterRemovesAlgorithm() {
        final SearchRegistry registry = new SearchRegistry();
        final PathAlgorithm algo = mock(PathAlgorithm.class);

        registry.register(algo);
        registry.unregister(algo);

        assertEquals(0, registry.all().size(), "The algorithm should be empty");
    }
}
