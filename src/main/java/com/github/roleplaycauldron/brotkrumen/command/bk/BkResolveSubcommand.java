package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.Visualizer;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * `/bk resolve` command.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class BkResolveSubcommand {

    private static final String PLAYER_ARGUMENT = "player";

    private static final String TARGET_ARGUMENT = "targets";

    private final BkCommandContext commandContext;

    /**
     * Initializes an instance of the BkResolveSubcommand class, which is responsible for
     * handling the `/bk resolve` subcommand in the Brotkrumen plugin. This subcommand
     * facilitates operations related to target resolution within the plugin's graphing
     * and visualization system.
     *
     * @param commandContext the shared context for handling commands within the
     *                       Brotkrumen plugin. It provides access to necessary services,
     *                       utilities, and execution resources required to build
     *                       and process the resolve subcommand.
     */
    public BkResolveSubcommand(final BkCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the resolve subcommand.
     *
     * @return subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> resolve() {
        return Commands.literal("resolve")
                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                        .suggests((context, builder) -> suggestOnlinePlayers(builder))
                        .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.greedyString())
                                .suggests((context, builder) -> suggestTargets(builder))
                                .executes(this::resolve)));
    }

    /**
     * Adds online player suggestions.
     *
     * @param builder suggestions builder
     * @return suggestions
     */
    private CompletableFuture<Suggestions> suggestOnlinePlayers(final SuggestionsBuilder builder) {
        final String remaining = builder.getRemainingLowerCase();
        final java.util.List<String> names = commandContext.plugin().getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
        BkCompletionSupport.onlinePlayers(names, remaining).forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * Adds canonical target suggestions.
     *
     * @param builder suggestions builder
     * @return suggestions
     */
    private CompletableFuture<Suggestions> suggestTargets(final SuggestionsBuilder builder) {
        BkCompletionSupport.resolveTargets(commandContext.graphService().getAllGraphs(), builder.getRemainingLowerCase())
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private int resolve(final CommandContext<CommandSourceStack> context) {
        final String playerName = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        final Player player = commandContext.plugin().getServer().getPlayerExact(playerName);
        if (player == null) {
            commandContext.send(context, "Player " + playerName + " is not online.");
            return 0;
        }
        final ResolveTargetParser.ParseResult parsed = commandContext.targetParser()
                .parse(StringArgumentType.getString(context, TARGET_ARGUMENT));
        if (!parsed.success()) {
            commandContext.send(context, parsed.error());
            return 0;
        }

        final UUID playerId = player.getUniqueId();
        final ResolveLocation location = ResolveLocation.from(player.getLocation());
        final ResolveOptions options = commandContext.resolveOptions();
        commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () ->
                resolveAsync(context, playerId, location, options, parsed.target()));
        commandContext.send(context, "Resolving path for " + player.getName() + "...");
        return Command.SINGLE_SUCCESS;
    }

    private void resolveAsync(final CommandContext<CommandSourceStack> context,
                              final UUID playerId, final ResolveLocation location,
                              final ResolveOptions options, final ResolveTarget target) {
        final ResolveResult result = target.mode() == ResolveTarget.Mode.GRAPH
                ? resolveGraphTarget(location, options, target)
                : resolveNodeTargets(location, options, target);
        commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(),
                () -> finishResolve(context, playerId, result));
    }

    private ResolveResult resolveGraphTarget(final ResolveLocation location,
                                             final ResolveOptions options,
                                             final ResolveTarget target) {
        final Graph graph = commandContext.resolveService().resolveGraph(target.graphKey()).orElse(null);
        if (graph == null) {
            return ResolveResult.failure("No graph found for " + target.graphKey() + ".");
        }
        final Node start = commandContext.resolveService()
                .nearestNode(graph, location, options.effectiveNearestNodeRadius())
                .orElse(null);
        if (start == null) {
            return ResolveResult.failure("No nearby node found in graph " + graph.getName() + ".");
        }
        return ResolveResult.graph(graph, options.backend());
    }

    private ResolveResult resolveNodeTargets(final ResolveLocation location,
                                             final ResolveOptions options,
                                             final ResolveTarget target) {
        final ResolveService.NodeTargetResolution resolution = commandContext.resolveService()
                .resolveNodeTargets(target.nodeIds());
        if (!resolution.success()) {
            return ResolveResult.failure(resolution.error());
        }
        final Graph graph = resolution.graph();
        final Node start = commandContext.resolveService()
                .nearestNode(graph, location, options.effectiveNearestNodeRadius())
                .orElse(null);
        if (start == null) {
            return ResolveResult.failure("No nearby node found in graph " + graph.getName() + ".");
        }
        final PathResult path = commandContext.resolveService()
                .findPath(graph, start.graphId(), new HashSet<>(resolution.nodeIds()));
        if (path.nodes().isEmpty()) {
            return ResolveResult.failure("No path found in graph " + graph.getName() + ".");
        }
        return ResolveResult.path(graph, path, options.backend());
    }

    private void finishResolve(final CommandContext<CommandSourceStack> context,
                               final UUID playerId, final ResolveResult result) {
        final Player player = commandContext.plugin().getServer().getPlayer(playerId);
        if (player == null) {
            return;
        }
        if (!result.success()) {
            commandContext.send(context, result.message());
            return;
        }
        final Visualizer visualizer = result.path() == null
                ? graphVisualizer(playerId, result)
                : pathVisualizer(playerId, result);
        commandContext.visualizerRegistry().replace(playerId, visualizer);
        commandContext.send(context, result.message());
    }

    private Visualizer graphVisualizer(final UUID playerId,
                                       final ResolveResult result) {
        if (result.backend() == ResolveBackend.BLOCK_DISPLAY) {
            return GraphVisualizerFactory.blockDisplayGraph(commandContext.plugin(), commandContext.loggerFactory(),
                    result.graph(), playerId);
        }
        return GraphVisualizerFactory.particleGraph(commandContext.plugin(), commandContext.loggerFactory(),
                result.graph(), playerId, commandContext.effectExecutor());
    }

    private Visualizer pathVisualizer(final UUID playerId,
                                      final ResolveResult result) {
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(result.graph());
        final GraphNetworkDesignProfile profile = GraphNetworkDesignProfile.defaults();
        if (result.backend() == ResolveBackend.BLOCK_DISPLAY) {
            return GraphVisualizerFactory.blockDisplayGuidedNetworkPath(commandContext.plugin(),
                    commandContext.loggerFactory(), network, result.path(), playerId, profile);
        }
        return GraphVisualizerFactory.particleGuidedNetworkPath(commandContext.plugin(),
                commandContext.loggerFactory(), network, result.path(), playerId,
                commandContext.effectExecutor(), profile);
    }

    @SuppressWarnings("PMD.CommentDefaultAccessModifier")
    private record ResolveResult(Graph graph, PathResult path, ResolveBackend backend, String message) {

        static ResolveResult graph(final Graph graph, final ResolveBackend backend) {
            return new ResolveResult(graph, null, backend, "Showing graph " + graph.getName() + ".");
        }

        static ResolveResult path(final Graph graph, final PathResult path, final ResolveBackend backend) {
            return new ResolveResult(graph, path, backend, "Showing path in graph " + graph.getName() + ".");
        }

        static ResolveResult failure(final String message) {
            return new ResolveResult(null, null, null, message);
        }

        boolean success() {
            return graph != null;
        }
    }
}
