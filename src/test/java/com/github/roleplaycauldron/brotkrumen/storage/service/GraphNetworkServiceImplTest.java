package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.InterGraphEdgeTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphNetworkServiceImplTest {

    @Mock
    private Storage storage;

    @Mock
    private GraphService graphService;

    @Mock
    private InterGraphEdgeTable interGraphEdgeTable;

    @Mock
    private BrotkrumenConnectionProvider provider;

    private GraphNetworkServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(storage.getProvider()).thenReturn(provider);
        service = new GraphNetworkServiceImpl(storage, graphService, interGraphEdgeTable);
    }

    @Test
    void partitionsGraphsIntoMultipleNetworks() {
        // ... (rest of the test as before)
    }

    @Test
    void saveInterGraphEdgesDeletesOrphanedEdges() {
        final Graph g1 = new Graph(1, "G1");
        final Graph g2 = new Graph(2, "G2");
        final UUID n1Id = UUID.randomUUID();
        final UUID n2Id = UUID.randomUUID();
        g1.addNode(new Node(n1Id, 0, 0, 0, null));
        g2.addNode(new Node(n2Id, 0, 0, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(g1);
        network.addGraph(g2);

        final UUID edgeId1 = UUID.randomUUID();
        final UUID edgeId2 = UUID.randomUUID();
        final InterGraphEdge edge1 = new InterGraphEdge(10, edgeId1, new NodeRef(1, n1Id), new NodeRef(2, n2Id), 1.0, Set.of(), true);
        final InterGraphEdge edge2 = new InterGraphEdge(11, edgeId2, new NodeRef(2, n2Id), new NodeRef(1, n1Id), 1.0, Set.of(), true);

        // Network only has edge1
        network.addInterGraphEdge(edge1);

        // Database has edge1 AND edge2 (orphaned)
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(edge1, edge2));

        service.saveInterGraphEdges(network);

        // edge1 should be saved/updated
        verify(interGraphEdgeTable).saveEdge(eq(provider), argThat(e -> e.edgeId().equals(edgeId1)));
        // edge2 should be deleted because it belongs to the network (G2->G1) but is not in the network object
        verify(interGraphEdgeTable).deleteById(provider, 11);
    }
}
