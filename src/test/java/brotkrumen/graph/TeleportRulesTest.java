package brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TeleportRulesTest {

    @Test
    void testTeleportDisabled() {
        TeleportRules rules = TeleportRules.disableTeleports();

        assertFalse(rules.isWarpingEnabled());
        assertFalse(rules.isLocalTeleportEnabled());
        assertEquals(0, rules.getWarps().size());
    }

    @Test
    void testTeleportEnabled() {
        Warp warp = new Warp("Spawn", 1, 10.0, true);
        TeleportRules rules = new TeleportRules(true, true, List.of(warp));

        assertTrue(rules.isWarpingEnabled());
        assertTrue(rules.isLocalTeleportEnabled());
        assertNotNull(rules.getWarp("Spawn"));

        Warp ruleWarp = rules.getWarp("Spawn").get();
        assertEquals(10.0, ruleWarp.cost());
        assertEquals(1, ruleWarp.targetNodeId());
    }
}
