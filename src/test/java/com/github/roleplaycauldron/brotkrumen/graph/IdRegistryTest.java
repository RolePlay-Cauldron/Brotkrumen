package com.github.roleplaycauldron.brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the IdRegistry class.
 */
class IdRegistryTest {

    @Test
    void testRegistryNodeIncrement() {
        final IdRegistry registry = new IdRegistry();
        assertEquals(1, registry.getNextNodeId(), "The next node id should be 1");
        assertEquals(2, registry.getNextNodeId(), "The next node id should be 2");
    }

    @Test
    void edgeAndNodeInUse() {
        final IdRegistry registry = new IdRegistry();

        registry.getNextNodeId();
        registry.getNextEdgeId();

        assertTrue(registry.isNodeInUse(1), "The node 1 should be in use");
        assertTrue(registry.isEdgeInUse(1), "The edge 1 should be in use");
    }

    @Test
    void testRegistryEdgeIncrement() {
        final IdRegistry registry = new IdRegistry();
        assertEquals(1, registry.getNextEdgeId(), "The next edge id should be 1");
        assertEquals(2, registry.getNextEdgeId(), "The next edge id should be 2");
    }

    @Test
    void testRegistryNodeReleaseAndReAcquire() {
        final IdRegistry registry = new IdRegistry();
        registry.getNextNodeId();
        assertEquals(2, registry.getNextNodeId(), "The next node id should be 2");

        registry.releaseNodeId(1);
        assertEquals(1, registry.getNextNodeId(), "The next edge id should be 1");
    }

    @Test
    void testRegistryEdgeReleaseAndReAcquire() {
        final IdRegistry registry = new IdRegistry();
        registry.getNextEdgeId();
        assertEquals(2, registry.getNextEdgeId(), "The next edge id should be 2");

        registry.releaseEdgeId(1);
        assertEquals(1, registry.getNextEdgeId(), "The next edge id should be 1");
    }

    @Test
    void testRegistryNodeSeedAcquire() {
        final IdRegistry registry = new IdRegistry(1, 1);
        registry.seedNodeIds(List.of(1, 3, 5));

        assertEquals(2, registry.getNextNodeId(), "The next node id should be 2");
        assertEquals(4, registry.getNextNodeId(), "The next node id should be 4");
    }

    @Test
    void testRegistryEdgeSeedAcquireWithStartValue() {
        final IdRegistry registry = new IdRegistry(10, 8);
        registry.seedEdgeIds(List.of(1, 3, 5));

        assertEquals(8, registry.getNextEdgeId(), "The next edge id should be 8");
    }

    @Test
    void testRegistryEdgeSeedAcquireWithStartValueAndSameSeedValue() {
        final IdRegistry registry = new IdRegistry(10, 5);
        registry.seedEdgeIds(List.of(1, 3, 5));

        assertEquals(6, registry.getNextEdgeId(), "The next edge id should be 6");
    }
}
