package com.github.roleplaycauldron.brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeleportRulesTest {

    private final UUID uuidOne = UUID.fromString("5e60eed2-3f0f-4695-9f86-5fe54006e44e");

    @Test
    void testTeleportDisabled() {
        final TeleportRules rules = TeleportRules.disableTeleports();

        final List<Object> actual = List.of(
                rules.isWarpingEnabled(),
                rules.isLocalTeleportEnabled(),
                rules.isInterGraphTeleportEnabled(),
                rules.isWarpTeleportEnabled(),
                rules.getWarps().size()
        );
        final List<Object> expected = List.of(false, false, false, false, 0);
        assertEquals(expected, actual, "Disabled rules should have all teleport kinds off and no warps");
    }

    @Test
    void testTeleportEnabled() {
        final Warp warp = new Warp("Spawn", uuidOne, 10.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(warp));

        final Optional<Warp> warpOpt = rules.getWarp("Spawn");

        final List<Object> actual = List.of(
                rules.isWarpingEnabled(),
                rules.isLocalTeleportEnabled(),
                rules.isInterGraphTeleportEnabled(),
                rules.isWarpTeleportEnabled(),
                warpOpt.isPresent(),
                warpOpt.map(Warp::cost).orElse(null),
                warpOpt.map(Warp::targetNodeId).orElse(null)
        );
        final List<Object> expected = List.of(true, true, true, true, true, 10.0, uuidOne);
        assertEquals(expected, actual, "Enabled rules + warp properties should match");
    }

    @Test
    void storesIndependentTeleportKindSwitches() {
        final TeleportRules rules = new TeleportRules(true, false, true, List.of());

        assertAll(
                () -> assertTrue(rules.isLocalTeleportEnabled(), "Local teleport switch should be independent"),
                () -> assertFalse(rules.isInterGraphTeleportEnabled(), "Intergraph teleport switch should be independent"),
                () -> assertTrue(rules.isWarpTeleportEnabled(), "Warp switch should be independent")
        );
    }

    @Test
    void filtersWarpsByPermissionMetadata() {
        final Warp open = new Warp("Open Warp", UUID.randomUUID(), 1.0D, true, false);
        final Warp byKey = new Warp("Test 123", UUID.randomUUID(), 1.0D, true, true);
        final Warp byNode = new Warp("No Key", uuidOne, 1.0D, true, true);
        final Warp denied = new Warp("Denied", UUID.randomUUID(), 1.0D, true, true);
        final Warp disabled = new Warp("Disabled", UUID.randomUUID(), 1.0D, false, false);

        final List<Warp> result = WarpPermissionHelper.allowedWarps(List.of(open, byKey, byNode, denied, disabled),
                permission -> "brotkrumen.warp.test_123".equals(permission)
                        || ("brotkrumen.warp." + uuidOne).equals(permission));

        assertEquals(List.of(open, byKey, byNode), result,
                "Filtering should keep open warps and permission-required warps with either accepted suffix");
    }
}
