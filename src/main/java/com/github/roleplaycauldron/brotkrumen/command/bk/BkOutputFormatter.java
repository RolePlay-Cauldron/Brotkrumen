package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Text formatting helpers for `/bk` output.
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public final class BkOutputFormatter {

    private static final String NO_GRAPHS_EXIST = "No graphs exist.";

    private static final String NO_NETWORKS_EXIST = "No networks exist.";

    private BkOutputFormatter() {
    }

    /**
     * Formats version diagnostics.
     *
     * @param pluginVersion plugin version
     * @param server        server version text
     * @param hooks         hook text
     * @param storage       storage text
     * @param schemaVersion schema version
     * @param onlinePlayers online player count
     * @param maxPlayers    max player count
     * @return diagnostics
     */
    public static String diagnostics(final String pluginVersion, final String server, final String hooks,
                                     final String storage, final int schemaVersion, final int onlinePlayers,
                                     final int maxPlayers) {
        return "Brotkrumen " + pluginVersion + "\n"
                + "Server: " + server + "\n"
                + "Hooks: " + hooks + "\n"
                + "Storage: " + storage + "\n"
                + "Storage schema: " + schemaVersion + "\n"
                + "Online players: " + onlinePlayers + "/" + maxPlayers;
    }

    /**
     * Formats graph list output.
     *
     * @param graphs graphs
     * @return output
     */
    public static String graphs(final Collection<Graph> graphs) {
        if (graphs.isEmpty()) {
            return NO_GRAPHS_EXIST;
        }
        return "Graphs: " + graphs.stream()
                .sorted(Comparator.comparingInt(Graph::getGraphId))
                .map(graph -> graph.getGraphId() + " " + graph.getName())
                .collect(Collectors.joining(", "));
    }

    /**
     * Formats network list output.
     *
     * @param networks networks
     * @return output
     */
    public static String networks(final Collection<GraphNetwork> networks) {
        if (networks.isEmpty()) {
            return NO_NETWORKS_EXIST;
        }
        final AtomicInteger index = new AtomicInteger(1);
        return networks.stream()
                .map(network -> "Network #" + index.getAndIncrement() + ": "
                        + network.getGraphs().size() + " graphs, "
                        + network.getInterGraphEdges().size() + " inter-graph edges")
                .collect(Collectors.joining("; "));
    }
}
