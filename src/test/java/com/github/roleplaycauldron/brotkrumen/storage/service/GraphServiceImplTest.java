package com.github.roleplaycauldron.brotkrumen.storage.service;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GraphServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GraphServiceImplTest {

    @Mock
    private Storage storage;

    @Mock
    private GraphTable graphTable;

    @Mock
    private BrotkrumenConnectionProvider provider;

    private GraphServiceImpl service;

    @BeforeEach
    void setUp() {
        when(storage.getProvider()).thenReturn(provider);
        service = new GraphServiceImpl(storage, graphTable);
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
}
