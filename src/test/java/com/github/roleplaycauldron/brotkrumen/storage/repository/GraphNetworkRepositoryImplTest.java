package com.github.roleplaycauldron.brotkrumen.storage.repository;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@code GraphNetworkRepositoryImpl}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class GraphNetworkRepositoryImplTest {

    @Mock
    private Storage storage;

    @Mock
    private GraphRepository graphRepository;

    @Mock
    private InterGraphEdgeTable interGraphEdgeTable;

    @Mock
    private BrotkrumenConnectionProvider provider;

    private GraphNetworkRepositoryImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(storage.getProvider()).thenReturn(provider);
        service = new GraphNetworkRepositoryImpl(storage, graphRepository, interGraphEdgeTable);
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

        when(graphRepository.getAllGraphs()).thenReturn(Set.of(graph1, graph2));
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(edge1, edge2), Set.of(edge1));
        service.saveInterGraphEdges(network);

        verify(interGraphEdgeTable).saveEdge(eq(provider), argThat(e -> e.edgeId().equals(edgeId1)));
        verify(interGraphEdgeTable).deleteById(provider, 11);
        verify(graphRepository).getAllGraphs();
        verify(interGraphEdgeTable, times(2)).getAllEdges(provider);
    }

    @Test
    void loadGraphNetworksIgnoresIsolatedGraphs() {
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(new Graph(1, "G1"), new Graph(2, "G2")));
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of());

        assertTrue(service.loadGraphNetworks().isEmpty(), "Isolated graphs should not create generated networks");
    }

    @Test
    void loadGraphNetworksRequiresConnectedGraphs() {
        final Graph graph1 = new Graph(1, "G1");
        final Graph graph2 = new Graph(2, "G2");
        final UUID node1 = UUID.randomUUID();
        final UUID node2 = UUID.randomUUID();
        graph1.addNode(new Node(node1, 0, 0, 0, null));
        graph2.addNode(new Node(node2, 0, 0, 0, null));
        final InterGraphEdge edge = new InterGraphEdge(10, UUID.randomUUID(), new NodeRef(1, node1),
                new NodeRef(2, node2), 1.0, Set.of(), true);
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(graph1, graph2));
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(edge));

        final GraphNetwork network = service.loadGraphNetworks().stream().findFirst().orElseThrow();

        assertEquals(2, network.getGraphs().size(), "Connected graphs should create one generated network");
    }

    @Test
    void loadsAndDeletesEdgesForGraphIds() {
        final Set<Integer> graphIds = Set.of(1, 2);
        final InterGraphEdge edge = new InterGraphEdge(10, UUID.randomUUID(),
                new NodeRef(1, UUID.randomUUID()), new NodeRef(2, UUID.randomUUID()), 1.0, Set.of(), true);
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(new Graph(1, "G1"), new Graph(2, "G2")));
        when(interGraphEdgeTable.findByGraphIds(provider, graphIds)).thenReturn(Set.of(edge));
        when(interGraphEdgeTable.deleteByGraphId(provider, 1)).thenReturn(2);
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of());

        assertEquals(Set.of(edge), service.loadInterGraphEdges(graphIds),
                "Targeted edge load should delegate to table graph-id lookup");
        assertEquals(2, service.deleteInterGraphEdgesForGraph(1),
                "Targeted edge delete should return table delete count");
        verify(graphRepository).getAllGraphs();
        verify(interGraphEdgeTable).getAllEdges(provider);
    }

    @Test
    void savesInterGraphEdgeCollectionWithExistingDbIds() {
        final UUID edgeId = UUID.randomUUID();
        final UUID sourceNodeId = UUID.randomUUID();
        final UUID targetNodeId = UUID.randomUUID();
        final InterGraphEdge existing = new InterGraphEdge(10, edgeId, new NodeRef(1, UUID.randomUUID()),
                new NodeRef(2, UUID.randomUUID()), 1.0, Set.of(), true);
        final InterGraphEdge replacement = new InterGraphEdge(edgeId, new NodeRef(1, sourceNodeId),
                new NodeRef(2, targetNodeId), 2.0,
                Set.of(), false);
        final Graph graph1 = new Graph(1, "G1");
        final Graph graph2 = new Graph(2, "G2");
        graph1.addNode(new Node(sourceNodeId, 0, 0, 0, null));
        graph2.addNode(new Node(targetNodeId, 0, 0, 0, null));
        when(graphRepository.getAllGraphs()).thenReturn(Set.of(graph1, graph2));
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(existing), Set.of(replacement));

        service.saveInterGraphEdges(Set.of(replacement));

        verify(interGraphEdgeTable).saveEdge(eq(provider), argThat(edge -> edge.dbId() == 10
                && edge.cost() == 2.0 && !edge.enabled()));
        verify(graphRepository).getAllGraphs();
        verify(interGraphEdgeTable, times(2)).getAllEdges(provider);
    }

    @Test
    void loadGraphNetworksCachesResult() {
        final Graph graph1 = new Graph(1, "G1");
        final Graph graph2 = new Graph(2, "G2");
        final UUID node1 = UUID.randomUUID();
        final UUID node2 = UUID.randomUUID();
        graph1.addNode(new Node(node1, 0, 0, 0, null));
        graph2.addNode(new Node(node2, 0, 0, 0, null));
        final InterGraphEdge edge = new InterGraphEdge(10, UUID.randomUUID(), new NodeRef(1, node1),
                new NodeRef(2, node2), 1.0, Set.of(), true);

        when(graphRepository.getAllGraphs()).thenReturn(Set.of(graph1, graph2));
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(edge));

        service.loadGraphNetworks();
        service.loadGraphNetworks();

        verify(graphRepository, times(1)).getAllGraphs();
        verify(interGraphEdgeTable, times(1)).getAllEdges(provider);
    }

    @Test
    void reloadGraphNetworksClearsAndRebuildsCache() {
        final Graph graph1 = new Graph(1, "G1");
        final Graph graph2 = new Graph(2, "G2");
        final UUID node1 = UUID.randomUUID();
        final UUID node2 = UUID.randomUUID();
        graph1.addNode(new Node(node1, 0, 0, 0, null));
        graph2.addNode(new Node(node2, 0, 0, 0, null));
        final InterGraphEdge edge = new InterGraphEdge(10, UUID.randomUUID(), new NodeRef(1, node1),
                new NodeRef(2, node2), 1.0, Set.of(), true);

        when(graphRepository.getAllGraphs()).thenReturn(Set.of(graph1, graph2));
        when(interGraphEdgeTable.getAllEdges(provider)).thenReturn(Set.of(edge));

        service.loadGraphNetworks();
        service.reloadGraphNetworks();

        verify(graphRepository, times(2)).getAllGraphs();
        verify(interGraphEdgeTable, times(2)).getAllEdges(provider);
    }
}
