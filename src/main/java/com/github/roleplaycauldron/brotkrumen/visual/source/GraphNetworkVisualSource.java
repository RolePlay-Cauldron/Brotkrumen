package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRoles;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

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
                final VisualNodeRole role = nodeRole(node);
                nodes.add(new VisualNode(new VisualNodeId(ref), ref, node, role));
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

        return new VisualGraphSnapshot(nodes, VisualEdgeCanonicalizer.canonicalize(edges), version());
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
                edge.flags(),
                VisualEdgeRoles.derive(VisualEdgeKind.LOCAL, edge.flags())
        );
    }

    private VisualEdge toInterGraphVisualEdge(final InterGraphEdge edge) {
        return new VisualEdge(
                new InterGraphVisualEdgeId(edge.edgeId()),
                edge.source(),
                edge.target(),
                VisualEdgeKind.INTER_GRAPH,
                edge.cost(),
                edge.flags(),
                VisualEdgeRoles.derive(VisualEdgeKind.INTER_GRAPH, edge.flags())
        );
    }

    private VisualNodeRole nodeRole(final Node node) {
        if (node.flags().contains(NodeFlag.WARP)) {
            return VisualNodeRole.WARP;
        }
        if (node.flags().contains(NodeFlag.INTERGRAPH_TELEPORT)) {
            return VisualNodeRole.INTERGRAPH_TELEPORT;
        }
        if (node.flags().contains(NodeFlag.LOCAL_TELEPORT)) {
            return VisualNodeRole.LOCAL_TELEPORT;
        }
        return VisualNodeRole.DEFAULT;
    }
}
