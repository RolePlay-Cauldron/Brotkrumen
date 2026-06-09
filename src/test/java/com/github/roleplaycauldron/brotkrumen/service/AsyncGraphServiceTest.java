package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsyncGraphServiceTest {

    private final Executor directExecutor = Runnable::run;

    @Test
    void graphReadCompletesThroughFuture() {
        final GraphRepository delegate = mock(GraphRepository.class);
        final Graph graph = new Graph(7, "Route");
        when(delegate.getGraphById(7)).thenReturn(Optional.of(graph));
        final AsyncGraphService service = new AsyncGraphService(delegate, directExecutor);

        assertEquals(Optional.of(graph), service.graphById(7).join(), "Future should expose delegate result");
    }

    @Test
    void graphWriteCompletesThroughFuture() {
        final GraphRepository delegate = mock(GraphRepository.class);
        final Graph graph = new Graph("Route");
        final AsyncGraphService service = new AsyncGraphService(delegate, directExecutor);

        service.saveGraph(graph).join();

        verify(delegate).saveGraph(graph);
    }

    @Test
    void graphFailureCompletesExceptionally() {
        final GraphRepository delegate = mock(GraphRepository.class);
        when(delegate.getAllGraphs()).thenThrow(new IllegalStateException("boom"));
        final AsyncGraphService service = new AsyncGraphService(delegate, directExecutor);

        assertThrows(CompletionException.class, () -> service.allGraphs().join(),
                "Future should complete exceptionally when delegate fails");
    }

    @Test
    void allGraphsCompletesThroughFuture() {
        final GraphRepository delegate = mock(GraphRepository.class);
        final Set<Graph> graphs = Set.of(new Graph("Route"));
        when(delegate.getAllGraphs()).thenReturn(graphs);
        final AsyncGraphService service = new AsyncGraphService(delegate, directExecutor);

        assertEquals(graphs, service.allGraphs().join(), "Future should expose all graphs");
    }
}
