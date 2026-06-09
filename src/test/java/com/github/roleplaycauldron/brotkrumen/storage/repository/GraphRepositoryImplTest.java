package com.github.roleplaycauldron.brotkrumen.storage.repository;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.GraphTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GraphRepositoryImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GraphRepositoryImplTest {

    @Mock
    private Storage storage;

    @Mock
    private GraphTable graphTable;

    @Mock
    private BrotkrumenConnectionProvider provider;

    private GraphRepositoryImpl service;

    @BeforeEach
    void setUp() {
        when(storage.getProvider()).thenReturn(provider);
        service = new GraphRepositoryImpl(storage, graphTable);
    }

    @Test
    void getGraphByNameReturnsDetachedCopy() {
        final Graph graph = new Graph(1, "Original");
        graph.addNode(new Node(UUID.randomUUID(), 1.0D, 2.0D, 3.0D, null));
        when(graphTable.findByName(provider, "Original")).thenReturn(Optional.of(graph));

        final Graph firstRead = service.getGraphByName("Original").orElseThrow();
        firstRead.setName("Changed");
        firstRead.addNode(new Node(UUID.randomUUID(), 4.0D, 5.0D, 6.0D, null));

        final Graph secondRead = service.getGraphByName("Original").orElseThrow();

        assertEquals("Original", secondRead.getName(), "Cached graph name should not be mutated by read result");
        assertEquals(1, secondRead.getNodes().size(), "Cached graph nodes should not be mutated by read result");
    }

    @Test
    void saveGraphRejectsDuplicateName() {
        final Graph graph = new Graph(2, "Duplicate");
        when(graphTable.isNameUsedByOtherGraph(provider, "Duplicate", 2)).thenReturn(true);

        assertThrows(StorageException.class, () -> service.saveGraph(graph),
                "Duplicate graph names should be rejected");
        verify(graphTable, never()).saveGraph(provider, graph);
    }

    @Test
    void saveNewGraphCachesPersistedGraphByGeneratedId() {
        final Graph newGraph = new Graph("Fresh");
        final Graph persisted = new Graph(3, "Fresh");
        when(graphTable.findByName(provider, "Fresh")).thenReturn(Optional.of(persisted));

        service.saveGraph(newGraph);

        assertEquals(3, service.getGraphByName("Fresh").orElseThrow().getGraphId(),
                "New graph saves should cache the persisted graph with its generated id");
        verify(graphTable).saveGraph(provider, newGraph);
    }

    @Test
    void saveGraphUpdatesOnlySavedCacheEntry() {
        final Graph graphA = new Graph(1, "A");
        final Graph graphB = new Graph(2, "B");
        final Graph updatedGraphA = new Graph(1, "A-updated");
        when(graphTable.getAllGraphs(provider)).thenReturn(Set.of(graphA, graphB));

        service.getAllGraphs();
        service.saveGraph(updatedGraphA);

        assertEquals(Set.of("A-updated", "B"), service.getAllGraphs().stream()
                        .map(Graph::getName)
                        .collect(java.util.stream.Collectors.toSet()),
                "Saving one graph should update only that cache entry and preserve other cached graphs");
        verify(graphTable).getAllGraphs(provider);
    }

    @Test
    void reloadGraphsInvalidatesCacheAndReloadsFromDatabase() {
        final Graph original = new Graph(1, "Original");
        final Graph reloaded = new Graph(1, "Reloaded");
        when(graphTable.getAllGraphs(provider)).thenReturn(Set.of(original), Set.of(reloaded));

        service.getAllGraphs();
        service.reloadGraphs();

        assertEquals(Set.of("Reloaded"), service.getAllGraphs().stream()
                        .map(Graph::getName)
                        .collect(java.util.stream.Collectors.toSet()),
                "Reload should replace cached graphs with fresh database state");
        verify(graphTable, times(2)).getAllGraphs(provider);
    }
}
