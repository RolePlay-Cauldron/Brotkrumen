package brotkrumen.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TeleportRulesTest {

    @Test
    void teleportDisabledTest() {
        TeleportRules rules = TeleportRules.disableTeleports();

        assertFalse(rules.isGlobalTeleportEnabled());
        assertFalse(rules.isLocalTeleportEnabled());
        assertEquals(Double.POSITIVE_INFINITY, rules.getGlobalTeleportCost());
        assertEquals(-1, rules.getGlobalTargetNodeId());
    }

    @Test
    void teleportEnabledTest() {
        TeleportRules rules = new TeleportRules(true, 10, 5.0, true);

        assertTrue(rules.isGlobalTeleportEnabled());
        assertTrue(rules.isLocalTeleportEnabled());
        assertEquals(5.0, rules.getGlobalTeleportCost());
        assertEquals(10, rules.getGlobalTargetNodeId());
    }
}
