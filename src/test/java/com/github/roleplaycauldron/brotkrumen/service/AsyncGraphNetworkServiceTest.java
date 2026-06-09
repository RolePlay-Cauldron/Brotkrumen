package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsyncGraphNetworkServiceTest {

    private final Executor directExecutor = Runnable::run;

    @Test
    void networkReadCompletesThroughFuture() {
        final GraphNetworkRepository delegate = mock(GraphNetworkRepository.class);
        final Collection<GraphNetwork> networks = Set.of(new GraphNetwork());
        when(delegate.loadGraphNetworks()).thenReturn(networks);
        final AsyncGraphNetworkService service = new AsyncGraphNetworkService(delegate, directExecutor);

        assertEquals(networks, service.graphNetworks().join(), "Future should expose graph networks");
    }

    @Test
    void networkWriteCompletesThroughFuture() {
        final GraphNetworkRepository delegate = mock(GraphNetworkRepository.class);
        final Collection<InterGraphEdge> edges = Set.of();
        final AsyncGraphNetworkService service = new AsyncGraphNetworkService(delegate, directExecutor);

        service.saveInterGraphEdges(edges).join();

        verify(delegate).saveInterGraphEdges(edges);
    }
}
