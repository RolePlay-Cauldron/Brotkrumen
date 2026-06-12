package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import com.github.roleplaycauldron.brotkrumen.util.DirectSimpleScheduler;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsyncGraphNetworkServiceTest {

    private final DirectSimpleScheduler scheduler = new DirectSimpleScheduler();

    @Test
    void networkReadCompletesThroughFuture() {
        final GraphNetworkRepository delegate = mock(GraphNetworkRepository.class);
        final Collection<GraphNetwork> networks = Set.of(new GraphNetwork());
        when(delegate.loadGraphNetworks()).thenReturn(networks);
        final AsyncGraphNetworkService service = new AsyncGraphNetworkService(delegate, scheduler);

        assertEquals(networks, service.graphNetworks().join(), "Future should expose graph networks");
    }

    @Test
    void networkWriteCompletesThroughFuture() {
        final GraphNetworkRepository delegate = mock(GraphNetworkRepository.class);
        final Collection<InterGraphEdge> edges = Set.of();
        final AsyncGraphNetworkService service = new AsyncGraphNetworkService(delegate, scheduler);

        service.saveInterGraphEdges(edges).join();

        verify(delegate).saveInterGraphEdges(edges);
    }
}
