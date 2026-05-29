package com.github.roleplaycauldron.brotkrumen.command.bk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResolveTargetParser}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResolveTargetParserTest {

    private final ResolveTargetParser parser = new ResolveTargetParser();

    @Test
    void parsesLongGraphTarget() {
        final ResolveTargetParser.ParseResult result = parser.parse(List.of("graph:sternchen"));

        assertTrue(result.success(), "Graph target should parse");
        assertEquals(ResolveTarget.Mode.GRAPH, result.target().mode(), "Mode should be graph");
        assertEquals("sternchen", result.target().graphKey(), "Graph key should be preserved");
    }

    @Test
    void parsesShortGraphTarget() {
        final ResolveTargetParser.ParseResult result = parser.parse(List.of("g:12"));

        assertTrue(result.success(), "Short graph target should parse");
        assertEquals("12", result.target().graphKey(), "Graph id should be preserved");
    }

    @Test
    void parsesNodeListWithLongAndShortPrefixes() {
        final UUID first = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");
        final UUID second = UUID.fromString("18a6d815-2c26-4fde-8179-e74baca4bb4e");

        final ResolveTargetParser.ParseResult result = parser.parse(List.of("node:" + first, "n:" + second));

        assertTrue(result.success(), "Node targets should parse");
        assertEquals(ResolveTarget.Mode.NODE_LIST, result.target().mode(), "Mode should be node list");
        assertEquals(List.of(first, second), result.target().nodeIds(), "Node ids should be preserved");
    }

    @Test
    void rejectsMixedGraphAndNodeTargets() {
        final UUID nodeId = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

        final ResolveTargetParser.ParseResult result = parser.parse(List.of("graph:sternchen", "node:" + nodeId));

        assertFalse(result.success(), "Mixed targets should fail");
        assertEquals("Graph and node targets cannot be mixed.", result.error(), "Error should explain ambiguity");
    }

    @Test
    void rejectsNetworkTargets() {
        final ResolveTargetParser.ParseResult result = parser.parse(List.of("network:main"));

        assertFalse(result.success(), "Network target should fail");
        assertEquals("Network targets are not supported by /bk resolve.", result.error(),
                "Error should explain unsupported target");
    }

    @Test
    void rejectsBareUuidTargets() {
        final UUID nodeId = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

        final ResolveTargetParser.ParseResult result = parser.parse(List.of(nodeId.toString()));

        assertFalse(result.success(), "Bare UUID targets should fail");
    }
}
