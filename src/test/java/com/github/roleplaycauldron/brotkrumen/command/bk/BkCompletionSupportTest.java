package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BkCompletionSupport}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class BkCompletionSupportTest {

    @Test
    void suggestsOnlinePlayersByPrefix() {
        final List<String> suggestions = BkCompletionSupport.onlinePlayers(List.of("Jannik", "Alex"), "ja");

        assertEquals(List.of("Jannik"), suggestions, "Only matching online players should be suggested");
    }

    @Test
    void suggestsOnlyCanonicalTargetPrefixes() {
        final List<String> suggestions = BkCompletionSupport.resolveTargets(List.of(), "");

        assertEquals(List.of("graph:", "node:"), suggestions, "Only long target prefixes should be suggested");
        assertFalse(suggestions.contains("g:"), "Short graph alias should not be suggested");
        assertFalse(suggestions.contains("n:"), "Short node alias should not be suggested");
    }

    @Test
    void suggestsGraphNamesAndIdsAfterGraphPrefix() {
        final Graph graph = new Graph(7, "sternchen");

        final List<String> suggestions = BkCompletionSupport.resolveTargets(List.of(graph), "graph:s");

        assertEquals(List.of("graph:sternchen"), suggestions, "Matching graph name should be suggested");
    }

    @Test
    void suggestsNodeUuidsAfterNodePrefix() {
        final UUID nodeId = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");
        final Graph graph = new Graph(7, "sternchen");
        graph.addNode(new Node(nodeId, 0.0D, 0.0D, 0.0D, UUID.randomUUID()));

        final List<String> suggestions = BkCompletionSupport.resolveTargets(List.of(graph), "node:5e");

        assertEquals(List.of("node:" + nodeId), suggestions, "Matching node UUID should be suggested");
    }
}
