package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Visual source for an editor workspace containing active and visible reference graphs.
 */
public class EditorWorkspaceVisualSource implements VisualGraphSource {

    private final Supplier<Graph> activeGraph;

    private final Supplier<Collection<Graph>> referenceGraphs;

    private final Supplier<Collection<InterGraphEdge>> interGraphEdges;

    private final Supplier<Long> workspaceVersion;

    /**
     * Creates an editor workspace source.
     *
     * @param activeGraph      active graph supplier
     * @param referenceGraphs  visible reference graph supplier
     * @param interGraphEdges  visible inter-graph edge supplier
     * @param workspaceVersion workspace state version supplier
     */
    public EditorWorkspaceVisualSource(final Supplier<Graph> activeGraph,
                                       final Supplier<Collection<Graph>> referenceGraphs,
                                       final Supplier<Collection<InterGraphEdge>> interGraphEdges,
                                       final Supplier<Long> workspaceVersion) {
        this.activeGraph = activeGraph;
        this.referenceGraphs = referenceGraphs;
        this.interGraphEdges = interGraphEdges;
        this.workspaceVersion = workspaceVersion;
    }

    @Override
    public VisualGraphSnapshot snapshot() {
        final List<VisualNode> nodes = new ArrayList<>();
        final List<VisualEdge> edges = new ArrayList<>();
        final Set<Integer> visibleGraphIds = new LinkedHashSet<>();

        addGraph(activeGraph.get(), nodes, edges, visibleGraphIds);
        for (final Graph graph : referenceGraphs.get()) {
            addGraph(graph, nodes, edges, visibleGraphIds);
        }
        for (final InterGraphEdge edge : interGraphEdges.get()) {
            if (edge.enabled() && visibleGraphIds.contains(edge.source().graphDbId())
                    && visibleGraphIds.contains(edge.target().graphDbId())) {
                edges.add(toInterGraphVisualEdge(edge));
            }
        }
        return new VisualGraphSnapshot(nodes, VisualEdgeCanonicalizer.canonicalize(edges), version());
    }

    @Override
    public long version() {
        long result = workspaceVersion.get();
        final Graph active = activeGraph.get();
        if (active != null) {
            result = (31 * result) + active.getModCount();
        }
        for (final Graph graph : referenceGraphs.get()) {
            result = (31 * result) + graph.getGraphId();
            result = (31 * result) + graph.getModCount();
        }
        return result;
    }

    private void addGraph(final Graph graph, final List<VisualNode> nodes, final List<VisualEdge> edges,
                          final Set<Integer> visibleGraphIds) {
        if (graph == null) {
            return;
        }
        final int visualGraphId = graph.getGraphId();
        visibleGraphIds.add(visualGraphId);
        for (final Node node : graph.getNodes()) {
            final NodeRef ref = new NodeRef(visualGraphId, node.graphId());
            nodes.add(new VisualNode(new VisualNodeId(ref), ref, node, nodeRole(node)));
        }
        for (final Edge edge : graph.getEdges()) {
            edges.add(new VisualEdge(
                    new LocalVisualEdgeId(visualGraphId, edge.edgeId()),
                    new NodeRef(visualGraphId, edge.source()),
                    new NodeRef(visualGraphId, edge.target()),
                    VisualEdgeKind.LOCAL,
                    edge.cost(),
                    edge.flags(),
                    VisualEdgeRoles.derive(VisualEdgeKind.LOCAL, edge.flags())
            ));
        }
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
