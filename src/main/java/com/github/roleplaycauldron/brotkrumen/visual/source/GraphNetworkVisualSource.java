package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual source for a graph network.
 */
public class GraphNetworkVisualSource implements VisualGraphSource {

    private final GraphNetwork network;

    /**
     * Creates a visual source.
     *
     * @param network network to expose
     */
    public GraphNetworkVisualSource(final GraphNetwork network) {
        this.network = network;
    }

    @Override
    public VisualGraphSnapshot snapshot() {
        final List<VisualNode> nodes = new ArrayList<>();
        final List<VisualEdge> edges = new ArrayList<>();

        for (final Graph graph : network.getGraphs()) {
            for (final Node node : graph.getNodes()) {
                final NodeRef ref = new NodeRef(graph.getGraphId(), node.graphId());
                nodes.add(new VisualNode(new VisualNodeId(ref), ref, node));
            }
            for (final Edge edge : graph.getEdges()) {
                edges.add(toLocalVisualEdge(graph, edge));
            }
        }

        for (final InterGraphEdge edge : network.getInterGraphEdges()) {
            if (edge.enabled()) {
                edges.add(toInterGraphVisualEdge(edge));
            }
        }

        return new VisualGraphSnapshot(nodes, edges, version());
    }

    @Override
    public long version() {
        long result = network.getModCount();
        for (final Graph graph : network.getGraphs()) {
            result = (31 * result) + graph.getGraphId();
            result = (31 * result) + graph.getModCount();
        }
        return result;
    }

    private VisualEdge toLocalVisualEdge(final Graph graph, final Edge edge) {
        return new VisualEdge(
                new LocalVisualEdgeId(graph.getGraphId(), edge.edgeId()),
                new NodeRef(graph.getGraphId(), edge.source()),
                new NodeRef(graph.getGraphId(), edge.target()),
                VisualEdgeKind.LOCAL,
                edge.cost(),
                edge.flags()
        );
    }

    private VisualEdge toInterGraphVisualEdge(final InterGraphEdge edge) {
        return new VisualEdge(
                new InterGraphVisualEdgeId(edge.edgeId()),
                edge.source(),
                edge.target(),
                VisualEdgeKind.INTER_GRAPH,
                edge.cost(),
                edge.flags()
        );
    }
}
