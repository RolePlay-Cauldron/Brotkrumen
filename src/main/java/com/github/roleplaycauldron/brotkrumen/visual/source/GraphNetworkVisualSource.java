package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.InterGraphVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        final Set<NodeRef> teleportEndpoints = teleportEndpoints();

        for (final Graph graph : network.getGraphs()) {
            for (final Node node : graph.getNodes()) {
                final NodeRef ref = new NodeRef(graph.getGraphId(), node.graphId());
                final VisualNodeRole role = teleportEndpoints.contains(ref)
                        ? VisualNodeRole.TELEPORT_ENDPOINT
                        : VisualNodeRole.DEFAULT;
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
                edge.flags(),
                edgeRole(edge.flags())
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
                VisualEdgeRole.INTER_GRAPH
        );
    }

    private VisualEdgeRole edgeRole(final Set<EdgeFlag> flags) {
        if (flags.contains(EdgeFlag.TELEPORT_GLOBAL)) {
            return VisualEdgeRole.GLOBAL_TELEPORT;
        }
        if (flags.contains(EdgeFlag.TELEPORT)) {
            return VisualEdgeRole.TELEPORT;
        }
        return VisualEdgeRole.DEFAULT_LOCAL;
    }

    private Set<NodeRef> teleportEndpoints() {
        final Set<NodeRef> result = new HashSet<>();
        for (final Graph graph : network.getGraphs()) {
            for (final Edge edge : graph.getEdges()) {
                if (edge.flags().contains(EdgeFlag.TELEPORT_GLOBAL)) {
                    result.add(new NodeRef(graph.getGraphId(), edge.target()));
                } else if (edge.flags().contains(EdgeFlag.TELEPORT)) {
                    result.add(new NodeRef(graph.getGraphId(), edge.source()));
                    result.add(new NodeRef(graph.getGraphId(), edge.target()));
                }
            }
        }
        return result;
    }
}
