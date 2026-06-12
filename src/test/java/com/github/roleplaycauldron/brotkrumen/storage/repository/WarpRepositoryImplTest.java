package com.github.roleplaycauldron.brotkrumen.storage.repository;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.WarpTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WarpRepositoryImpl}.
 */
@ExtendWith(MockitoExtension.class)
class WarpRepositoryImplTest {

    @Mock
    private Storage storage;

    @Mock
    private WarpTable warpTable;

    @Mock
    private BrotkrumenConnectionProvider provider;

    private WarpRepositoryImpl service;

    @BeforeEach
    void setUp() {
        when(storage.getProvider()).thenReturn(provider);
        service = new WarpRepositoryImpl(storage, warpTable);
    }

    @Test
    void returnsMultipleKeysForOneTarget() {
        final UUID target = UUID.randomUUID();
        final Set<Warp> warps = Set.of(new Warp("spawn", target, 1.0D, true, true),
                new Warp("hub", target, 2.0D, true, false));
        when(warpTable.findByTargetNodeId(provider, target)).thenReturn(warps);

        assertSame(warps, service.getWarpsTargeting(target), "Target query should expose every stored warp");
    }

    @Test
    void updatesWarpMetadataThroughSave() {
        final Warp warp = new Warp("spawn", UUID.randomUUID(), 7.0D, false, false);

        service.saveWarp(warp);
        when(warpTable.findByKey(provider, "spawn")).thenReturn(Optional.of(warp));

        assertEquals(Optional.of(warp), service.getWarp("spawn"), "Read should expose updated metadata");
        verify(warpTable).saveWarp(provider, warp);
    }

    @Test
    void filtersManagedReadsByRequestedTargets() {
        final UUID included = UUID.randomUUID();
        final Warp warp = new Warp("spawn", included, 1.0D, true, true);
        when(warpTable.getAllWarps(provider)).thenReturn(Set.of(warp,
                new Warp("other", UUID.randomUUID(), 1.0D, true, true)));

        assertEquals(Set.of(warp), service.getWarpsTargeting(List.of(included)),
                "Batch target query should keep matching managed warps");
        assertTrue(service.getWarpsTargeting(List.of()).isEmpty(), "Empty target query should not read storage");
    }

    @Test
    void removesWarpsByTargetNodeIds() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        when(warpTable.deleteByTargetNodeIds(provider, List.of(first, second))).thenReturn(2);

        assertEquals(2, service.removeWarpsTargeting(List.of(first, second)),
                "Batch warp removal should return table delete count");
    }
}
