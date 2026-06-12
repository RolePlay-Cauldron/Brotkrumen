package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.repository.WarpRepository;
import com.github.roleplaycauldron.brotkrumen.util.DirectSimpleScheduler;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsyncWarpServiceTest {

    private final DirectSimpleScheduler scheduler = new DirectSimpleScheduler();

    @Test
    void warpReadCompletesThroughFuture() {
        final WarpRepository delegate = mock(WarpRepository.class);
        final Warp warp = new Warp("spawn", UUID.randomUUID(), 1.0D, true, false);
        when(delegate.getWarp("spawn")).thenReturn(Optional.of(warp));
        final AsyncWarpService service = new AsyncWarpService(delegate, scheduler);

        assertEquals(Optional.of(warp), service.warp("spawn").join(), "Future should expose warp lookup");
    }

    @Test
    void warpWriteCompletesThroughFuture() {
        final WarpRepository delegate = mock(WarpRepository.class);
        final Warp warp = new Warp("spawn", UUID.randomUUID(), 1.0D, true, false);
        final AsyncWarpService service = new AsyncWarpService(delegate, scheduler);

        service.saveWarp(warp).join();

        verify(delegate).saveWarp(warp);
    }

    @Test
    void warpFailureCompletesExceptionally() {
        final WarpRepository delegate = mock(WarpRepository.class);
        when(delegate.getManagedWarps()).thenThrow(new IllegalStateException("boom"));
        final AsyncWarpService service = new AsyncWarpService(delegate, scheduler);

        assertThrows(CompletionException.class, () -> service.managedWarps().join(),
                "Future should complete exceptionally when delegate fails");
    }

    @Test
    void managedWarpsCompletesThroughFuture() {
        final WarpRepository delegate = mock(WarpRepository.class);
        final Set<Warp> warps = Set.of(new Warp("spawn", UUID.randomUUID(), 1.0D, true, false));
        when(delegate.getManagedWarps()).thenReturn(warps);
        final AsyncWarpService service = new AsyncWarpService(delegate, scheduler);

        assertEquals(warps, service.managedWarps().join(), "Future should expose managed warps");
    }
}
