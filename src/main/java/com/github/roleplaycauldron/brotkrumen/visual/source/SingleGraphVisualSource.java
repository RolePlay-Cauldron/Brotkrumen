package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.LocalVisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeId;

import java.util.List;

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
        final List<VisualNode> nodes = graph.getNodes().stream()
                .map(this::toVisualNode)
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

    private VisualNode toVisualNode(final Node node) {
        final NodeRef ref = new NodeRef(graph.getGraphId(), node.graphId());
        return new VisualNode(new VisualNodeId(ref), ref, node);
    }

    private VisualEdge toVisualEdge(final Edge edge) {
        return new VisualEdge(
                new LocalVisualEdgeId(graph.getGraphId(), edge.edgeId()),
                new NodeRef(graph.getGraphId(), edge.source()),
                new NodeRef(graph.getGraphId(), edge.target()),
                VisualEdgeKind.LOCAL,
                edge.cost(),
                edge.flags()
        );
    }
}
