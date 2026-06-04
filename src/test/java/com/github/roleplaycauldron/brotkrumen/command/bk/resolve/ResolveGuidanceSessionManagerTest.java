package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResolveGuidanceSessionManager}.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ResolveGuidanceSessionManagerTest {

    @Test
    void cancelReturnsFalseWhenNoSessionExists() {
        final ResolveGuidanceSessionManager manager = new ResolveGuidanceSessionManager(mock(VisualizerRegistry.class));

        assertFalse(manager.cancel(UUID.randomUUID()), "Missing sessions should report not cancelled");
    }

    @Test
    void replacementCancelsActiveGuidance() {
        final UUID playerId = UUID.randomUUID();
        final VisualizerRegistry registry = mock(VisualizerRegistry.class);
        final ResolveGuidanceSessionManager manager = new ResolveGuidanceSessionManager(registry);
        final long initialToken = manager.replaceWithPending(playerId);
        assertTrue(manager.activate(playerId, initialToken), "Initial session should become active");

        final long replacementToken = manager.replaceWithPending(playerId);

        assertNotEquals(initialToken, replacementToken, "Replacement session should use a new token");
        assertTrue(manager.isCurrent(playerId, replacementToken), "Replacement should become current session");
        verify(registry).unregister(playerId);
    }

    @Test
    void staleTokensAreRejectedAfterReplacement() {
        final UUID playerId = UUID.randomUUID();
        final ResolveGuidanceSessionManager manager = new ResolveGuidanceSessionManager(mock(VisualizerRegistry.class));
        final long staleToken = manager.replaceWithPending(playerId);
        final long currentToken = manager.replaceWithPending(playerId);

        assertFalse(manager.isCurrent(playerId, staleToken), "Older token should become stale");
        assertFalse(manager.activate(playerId, staleToken), "Stale token should not activate session");
        assertTrue(manager.isCurrent(playerId, currentToken), "Newest token should remain current");
    }

    @Test
    void completionIsReportedOnlyOncePerToken() {
        final UUID playerId = UUID.randomUUID();
        final ResolveGuidanceSessionManager manager = new ResolveGuidanceSessionManager(mock(VisualizerRegistry.class));
        final long token = manager.replaceWithPending(playerId);
        assertTrue(manager.activate(playerId, token), "Session should activate before completion");

        assertTrue(manager.markCompleted(playerId, token), "First completion should be accepted");
        assertFalse(manager.markCompleted(playerId, token), "Duplicate completion should be ignored");
    }

    @Test
    void cleanupUsesTokenSafety() {
        final UUID playerId = UUID.randomUUID();
        final VisualizerRegistry registry = mock(VisualizerRegistry.class);
        final ResolveGuidanceSessionManager manager = new ResolveGuidanceSessionManager(registry);
        final long tokenOne = manager.replaceWithPending(playerId);
        assertTrue(manager.activate(playerId, tokenOne), "Original session should activate");
        final long tokenTwo = manager.replaceWithPending(playerId);

        assertFalse(manager.clearIfCurrent(playerId, tokenOne),
                "Cleanup of replaced token should not remove newer guidance");
        assertTrue(manager.isCurrent(playerId, tokenTwo), "Newer token should remain active");

        assertTrue(manager.activate(playerId, tokenTwo), "Replacement session should activate");
        assertTrue(manager.clearIfCurrent(playerId, tokenTwo), "Cleanup should remove current token");
        verify(registry, times(2)).unregister(playerId);
    }
}
