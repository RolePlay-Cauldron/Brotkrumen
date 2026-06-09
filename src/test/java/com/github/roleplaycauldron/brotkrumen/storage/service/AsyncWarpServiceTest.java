package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsyncWarpServiceTest {

    private final Executor directExecutor = Runnable::run;

    @Test
    void warpReadCompletesThroughFuture() {
        final WarpService delegate = mock(WarpService.class);
        final Warp warp = new Warp("spawn", UUID.randomUUID(), 1.0D, true, false);
        when(delegate.getWarp("spawn")).thenReturn(Optional.of(warp));
        final AsyncWarpService service = new AsyncWarpService(delegate, directExecutor);

        assertEquals(Optional.of(warp), service.warp("spawn").join(), "Future should expose warp lookup");
    }

    @Test
    void warpWriteCompletesThroughFuture() {
        final WarpService delegate = mock(WarpService.class);
        final Warp warp = new Warp("spawn", UUID.randomUUID(), 1.0D, true, false);
        final AsyncWarpService service = new AsyncWarpService(delegate, directExecutor);

        service.saveWarp(warp).join();

        verify(delegate).saveWarp(warp);
    }

    @Test
    void warpFailureCompletesExceptionally() {
        final WarpService delegate = mock(WarpService.class);
        when(delegate.getManagedWarps()).thenThrow(new IllegalStateException("boom"));
        final AsyncWarpService service = new AsyncWarpService(delegate, directExecutor);

        assertThrows(CompletionException.class, () -> service.managedWarps().join(),
                "Future should complete exceptionally when delegate fails");
    }

    @Test
    void managedWarpsCompletesThroughFuture() {
        final WarpService delegate = mock(WarpService.class);
        final Set<Warp> warps = Set.of(new Warp("spawn", UUID.randomUUID(), 1.0D, true, false));
        when(delegate.getManagedWarps()).thenReturn(warps);
        final AsyncWarpService service = new AsyncWarpService(delegate, directExecutor);

        assertEquals(warps, service.managedWarps().join(), "Future should expose managed warps");
    }
}
