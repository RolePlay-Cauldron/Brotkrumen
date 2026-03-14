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

/**
 * Unit test class for {@code GraphNetworkServiceImpl}.
 */
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
    void saveInterGraphEdgesDeletesOrphanedEdges() {
        final Graph graph1 = new Graph(1, "G1");
        final Graph graph2 = new Graph(2, "G2");
        final UUID n1Id = UUID.randomUUID();
        final UUID n2Id = UUID.randomUUID();
        graph1.addNode(new Node(n1Id, 0, 0, 0, null));
        graph2.addNode(new Node(n2Id, 0, 0, 0, null));

        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph1);
        network.addGraph(graph2);

        final UUID edgeId1 = UUID.randomUUID();
        final UUID edgeId2 = UUID.randomUUID();
        final InterGraphEdge edge1 = new InterGraphEdge(10, edgeId1, new NodeRef(1, n1Id), new NodeRef(2, n2Id), 1.0, Set.of(), true);
        final InterGraphEdge edge2 = new InterGraphEdge(11, edgeId2, new NodeRef(2, n2Id), new NodeRef(1, n1Id), 1.0, Set.of(), true);
        network.addInterGraphEdge(edge1);

        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(edge1, edge2));
        service.saveInterGraphEdges(network);

        verify(interGraphEdgeTable).saveEdge(eq(provider), argThat(e -> e.edgeId().equals(edgeId1)));
        verify(interGraphEdgeTable).deleteById(provider, 11);
    }
}
