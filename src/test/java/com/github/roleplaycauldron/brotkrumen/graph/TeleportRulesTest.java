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
                rules.getWarps().size()
        );
        final List<Object> expected = List.of(false, false, 0);
        assertEquals(expected, actual, "Disabled rules should have warping/local off and no warps");
    }

    @Test
    void testTeleportEnabled() {
        final Warp warp = new Warp("Spawn", uuidOne, 10.0, true);
        final TeleportRules rules = new TeleportRules(true, true, List.of(warp));

        final Optional<Warp> warpOpt = rules.getWarp("Spawn");

        final List<Object> actual = List.of(
                rules.isWarpingEnabled(),
                rules.isLocalTeleportEnabled(),
                warpOpt.isPresent(),
                warpOpt.map(Warp::cost).orElse(null),
                warpOpt.map(Warp::targetNodeId).orElse(null)
        );
        final List<Object> expected = List.of(true, true, true, 10.0, uuidOne);
        assertEquals(expected, actual, "Enabled rules + warp properties should match");
    }
}
