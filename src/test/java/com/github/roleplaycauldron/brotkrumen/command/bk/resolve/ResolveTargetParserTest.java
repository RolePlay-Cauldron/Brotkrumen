package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResolveTargetParser}.
 */
class ResolveTargetParserTest {

    private final ResolveTargetParser parser = new ResolveTargetParser();

    @Test
    void parsesLongGraphTarget() throws TargetParseException {
        final ResolveTarget target = parser.parse(List.of("graph:sternchen"));

        assertEquals(ResolveTarget.Mode.GRAPH, target.mode(), "Mode should be graph");
        assertEquals("sternchen", target.graphKey(), "Graph key should be preserved");
    }

    @Test
    void parsesShortGraphTarget() throws TargetParseException {
        final ResolveTarget target = parser.parse(List.of("g:12"));

        assertEquals("12", target.graphKey(), "Graph id should be preserved");
    }

    @Test
    void parsesNodeListWithLongAndShortPrefixes() throws TargetParseException {
        final UUID first = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");
        final UUID second = UUID.fromString("18a6d815-2c26-4fde-8179-e74baca4bb4e");

        final ResolveTarget target = parser.parse(List.of("node:" + first, "n:" + second));

        assertEquals(ResolveTarget.Mode.NODE_LIST, target.mode(), "Mode should be node list");
        assertEquals(List.of(first, second), target.nodeIds(), "Node ids should be preserved");
    }

    @Test
    void rejectsMixedGraphAndNodeTargets() {
        final UUID nodeId = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

        final TargetParseException exception = assertThrows(TargetParseException.class,
                () -> parser.parse(List.of("graph:sternchen", "node:" + nodeId)));

        assertEquals("commands.bk.resolve.parse.error.mixedTargetModes", exception.getErrorKey(),
                "Error key should explain ambiguity");
    }

    @Test
    void rejectsNetworkTargets() {
        final TargetParseException exception = assertThrows(TargetParseException.class,
                () -> parser.parse(List.of("network:main")));

        assertEquals("commands.bk.resolve.parse.error.networkNotSupported", exception.getErrorKey(),
                "Error key should explain unsupported target");
    }

    @Test
    void rejectsBareUuidTargets() {
        final UUID nodeId = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

        assertThrows(TargetParseException.class,
                () -> parser.parse(List.of(nodeId.toString())), "Should reject bare UUID");
    }
}
