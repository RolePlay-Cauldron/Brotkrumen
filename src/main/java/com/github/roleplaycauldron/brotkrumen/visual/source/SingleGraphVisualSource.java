package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRoles;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Visual source for one graph.
 */
public class SingleGraphVisualSource implements VisualGraphSource {

    private final Graph graph;

    /**
     * Creates a visual source.
     *
     * @param graph graph to expose
     */
    public SingleGraphVisualSource(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public VisualGraphSnapshot snapshot() {
        final Set<UUID> teleportEndpoints = teleportEndpoints();
        final List<VisualNode> nodes = graph.getNodes().stream()
                .map(node -> toVisualNode(node, teleportEndpoints))
                .toList();
        final List<VisualEdge> edges = graph.getEdges().stream()
                .map(this::toVisualEdge)
                .toList();
        return new VisualGraphSnapshot(nodes, edges, version());
    }

    @Override
    public long version() {
        return graph.getModCount();
    }

    private VisualNode toVisualNode(final Node node, final Set<UUID> teleportEndpoints) {
        final NodeRef ref = new NodeRef(graph.getGraphId(), node.graphId());
        final VisualNodeRole role = teleportEndpoints.contains(node.graphId())
                ? VisualNodeRole.TELEPORT_ENDPOINT
                : VisualNodeRole.DEFAULT;
        return new VisualNode(new VisualNodeId(ref), ref, node, role);
    }

    private VisualEdge toVisualEdge(final Edge edge) {
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

    private Set<UUID> teleportEndpoints() {
        final Set<UUID> result = new HashSet<>();
        for (final Edge edge : graph.getEdges()) {
            if (edge.flags().contains(EdgeFlag.TELEPORT_GLOBAL)) {
                result.add(edge.target());
            } else if (edge.flags().contains(EdgeFlag.TELEPORT)) {
                result.add(edge.source());
                result.add(edge.target());
            }
        }
        return result;
    }
}
