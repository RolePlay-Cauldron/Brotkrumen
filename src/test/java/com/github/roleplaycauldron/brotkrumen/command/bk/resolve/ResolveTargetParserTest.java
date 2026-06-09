package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class ResolveTargetParserTest {

    private final ResolveTargetParser parser = new ResolveTargetParser();

    @Test
    void parsesGraphTarget() throws TargetParseException {
        final ResolveTarget target = parser.parse("graph:sternchen");
        assertEquals(ResolveTarget.Mode.GRAPH, target.mode());
        assertEquals("sternchen", target.graphKey());
        assertNull(target.teleportRules());
    }

    @Test
    void parsesGraphTargetWithShortPrefix() throws TargetParseException {
        final ResolveTarget target = parser.parse("g:sternchen");
        assertEquals(ResolveTarget.Mode.GRAPH, target.mode());
        assertEquals("sternchen", target.graphKey());
    }

    @Test
    void parsesNodeTarget() throws TargetParseException {
        final UUID nodeId = UUID.randomUUID();
        final ResolveTarget target = parser.parse("node:" + nodeId);
        assertEquals(ResolveTarget.Mode.NODE_LIST, target.mode());
        assertEquals(List.of(nodeId), target.nodeIds());
        assertNull(target.teleportRules());
    }

    @Test
    void parsesTeleportRules() throws TargetParseException {
        final ResolveTarget target = parser.parse("graph:sternchen teleport:LOCAL_TP_ONLY");
        assertEquals("sternchen", target.graphKey());
        assertEquals("LOCAL_TP_ONLY", target.teleportRules());
    }

    @Test
    void parsesTeleportRulesWithShortPrefix() throws TargetParseException {
        final ResolveTarget target = parser.parse("graph:sternchen tp:WARPS_ONLY");
        assertEquals("WARPS_ONLY", target.teleportRules());
    }

    @Test
    void parsesTeleportRulesOnly() throws TargetParseException {
        // This should fail because graph/node is missing
        assertThrows(TargetParseException.class, () -> parser.parse("teleport:DISABLED"));
    }

    @Test
    void failsOnMultipleTeleportRules() {
        assertThrows(TargetParseException.class, () -> parser.parse("graph:s tp:DISABLED teleport:WARPS"));
    }

    @Test
    void failsOnEmptyTeleportRules() {
        assertThrows(TargetParseException.class, () -> parser.parse("graph:s tp:"));
    }
}
