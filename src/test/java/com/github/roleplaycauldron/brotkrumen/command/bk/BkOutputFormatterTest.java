package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BkOutputFormatter}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BkOutputFormatterTest {

    @Test
    void diagnosticsIncludeImportantFields() {
        final String diagnostics = BkOutputFormatter.diagnostics("1.0.0", "Paper 1.21", "none", "sqlite", 1, 2, 20);

        assertTrue(diagnostics.contains("Brotkrumen 1.0.0"), "Plugin version should be included");
        assertTrue(diagnostics.contains("Server: Paper 1.21"), "Server version should be included");
        assertTrue(diagnostics.contains("Hooks: none"), "Hooks should be included");
        assertTrue(diagnostics.contains("Storage: sqlite"), "Storage should be included");
        assertTrue(diagnostics.contains("Storage schema: 1"), "Schema version should be included");
        assertTrue(diagnostics.contains("Online players: 2/20"), "Online count should be included");
    }

    @Test
    void formatsEmptyGraphList() {
        assertEquals("No graphs exist.", BkOutputFormatter.graphs(List.of()), "Empty graph list should be explicit");
    }

    @Test
    void formatsGraphList() {
        assertEquals("Graphs: 2 sternchen", BkOutputFormatter.graphs(List.of(new Graph(2, "sternchen"))),
                "Graph id and name should be listed");
    }

    @Test
    void formatsEmptyNetworkList() {
        assertEquals("No networks exist.", BkOutputFormatter.networks(List.of()),
                "Empty network list should be explicit");
    }

    @Test
    void formatsNetworkList() {
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(new Graph(1, "sternchen"));

        assertEquals("Network #1: 1 graphs, 0 inter-graph edges", BkOutputFormatter.networks(List.of(network)),
                "Network summary should include graph and edge counts");
    }
}
