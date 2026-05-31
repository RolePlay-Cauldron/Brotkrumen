package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.visual.GraphVisualizerFactory;
import com.github.roleplaycauldron.brotkrumen.visual.GuidedPathCompletionVisualizer;
import com.github.roleplaycauldron.brotkrumen.visual.Visualizer;
import com.github.roleplaycauldron.brotkrumen.visual.design.GraphNetworkDesignProfile;
import com.github.roleplaycauldron.brotkrumen.visual.design.ProfileGraphDesignResolver;
import com.github.roleplaycauldron.brotkrumen.visual.render.BlockDisplayGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.render.ParticleGraphRenderer;
import com.github.roleplaycauldron.brotkrumen.visual.source.GraphNetworkVisualSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathOptions;
import com.github.roleplaycauldron.brotkrumen.visual.source.GuidedPathVisualGraphSource;
import com.github.roleplaycauldron.brotkrumen.visual.source.ViewerLocationSource;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * `/bk resolve` command.
 */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
public final class BkResolveSubcommand {

    private static final String CANCEL_LITERAL = "cancel";

    private static final String PLAYER_ARGUMENT = "player";

    private static final String TARGET_ARGUMENT = "targets";

    private static final String GUIDED_PATH_CONFIG = "visualizer.guidedPath";

    private static final long NO_CLEANUP_DELAY_TICKS = 0L;

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
                .then(Commands.literal(CANCEL_LITERAL)
                        .executes(this::cancelOwnGuidance)
                        .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> suggestOnlinePlayers(builder))
                                .executes(this::cancelGuidance)))
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
        final List<String> names = commandContext.plugin().getServer().getOnlinePlayers().stream()
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
        final Player player = resolveTargetPlayer(context);
        if (player == null) {
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
        final long token = commandContext.sessionManager().replaceWithPending(playerId);
        commandContext.plugin().getServer().getScheduler().runTaskAsynchronously(commandContext.plugin(), () ->
                resolveAsync(context, playerId, token, location, options, parsed.target()));
        commandContext.send(context, "Resolving path for " + player.getName() + "...");
        return Command.SINGLE_SUCCESS;
    }

    private void resolveAsync(final CommandContext<CommandSourceStack> context,
                              final UUID playerId, final long token, final ResolveLocation location,
                              final ResolveOptions options, final ResolveTarget target) {
        final ResolveResult result = target.mode() == ResolveTarget.Mode.GRAPH
                ? resolveGraphTarget(location, options, target)
                : resolveNodeTargets(location, options, target);
        commandContext.plugin().getServer().getScheduler().runTask(commandContext.plugin(),
                () -> finishResolve(context, playerId, token, options, result));
    }

    private ResolveResult resolveGraphTarget(final ResolveLocation location,
                                             final ResolveOptions options,
                                             final ResolveTarget target) {
        final Graph graph = commandContext.resolveService().resolveGraph(target.graphKey()).orElse(null);
        if (graph == null) {
            return ResolveResult.failure("No graph found for " + target.graphKey() + ".");
        }
        final NodeRef start = commandContext.resolveService()
                .nearestNodeRef(commandContext.graphService().getAllGraphs(), location, options.effectiveNearestNodeRadius())
                .orElse(null);
        if (start == null) {
            return ResolveResult.failure("No nearby graph node found.");
        }
        if (start.graphDbId() == graph.getGraphId()) {
            return ResolveResult.graph(graph, options.backend());
        }
        final List<GraphNetwork> networks = commandContext.resolveService().loadGraphNetworks();
        final GraphNetwork network = commandContext.resolveService()
                .networkContaining(networks, List.of(start.graphDbId(), graph.getGraphId()))
                .orElse(null);
        if (network == null) {
            return ResolveResult.failure("No graph network connects your current graph to " + graph.getName() + ".");
        }
        final PathResult path = commandContext.resolveService().findPath(network, start, graph.getGraphId());
        if (path.nodes().isEmpty()) {
            return ResolveResult.failure("No path found to graph " + graph.getName() + ".");
        }
        return ResolveResult.path(network, path, options.backend(), "Showing path to graph " + graph.getName() + ".");
    }

    private ResolveResult resolveNodeTargets(final ResolveLocation location,
                                             final ResolveOptions options,
                                             final ResolveTarget target) {
        final ResolveService.NodeRefTargetResolution resolution = commandContext.resolveService()
                .resolveNodeRefTargets(target.nodeIds());
        if (!resolution.success()) {
            return ResolveResult.failure(resolution.error());
        }
        final NodeRef start = commandContext.resolveService()
                .nearestNodeRef(commandContext.graphService().getAllGraphs(), location, options.effectiveNearestNodeRadius())
                .orElse(null);
        if (start == null) {
            return ResolveResult.failure("No nearby graph node found.");
        }
        final Collection<Integer> requiredGraphIds = Stream.concat(
                        Stream.of(start.graphDbId()),
                        resolution.nodeRefs().stream().map(NodeRef::graphDbId))
                .collect(Collectors.toSet());
        final GraphNetwork network = requiredGraphIds.size() == 1
                ? commandContext.resolveService().singleGraphNetwork(
                commandContext.graphService().getGraphById(start.graphDbId()).orElseThrow())
                : commandContext.resolveService()
                .networkContaining(commandContext.resolveService().loadGraphNetworks(), requiredGraphIds)
                .orElse(null);
        if (network == null) {
            return ResolveResult.failure("No graph network connects your current graph to the requested node target.");
        }
        final PathResult path = commandContext.resolveService()
                .findPath(network, start, resolution.nodeRefs());
        if (path.nodes().isEmpty()) {
            return ResolveResult.failure("No path found to requested node target.");
        }
        return ResolveResult.path(network, path, options.backend(), "Showing path to requested node target.");
    }

    private void finishResolve(final CommandContext<CommandSourceStack> context,
                               final UUID playerId, final long token, final ResolveOptions options,
                               final ResolveResult result) {
        if (!commandContext.sessionManager().isCurrent(playerId, token)) {
            return;
        }
        final Player player = commandContext.plugin().getServer().getPlayer(playerId);
        if (player == null) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            return;
        }
        if (!result.success()) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            commandContext.send(context, result.message());
            return;
        }
        final Visualizer visualizer = result.path() == null
                ? graphVisualizer(playerId, result)
                : pathVisualizer(playerId, token, options, result);
        if (!commandContext.sessionManager().activate(playerId, token)) {
            visualizer.shutdown();
            return;
        }
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
                                      final long token,
                                      final ResolveOptions options,
                                      final ResolveResult result) {
        final GraphNetworkDesignProfile profile = GraphNetworkDesignProfile.defaults();
        final GuidedPathOptions guidedPathOptions = resolveGuidedPathOptions(options);
        final GuidedPathVisualGraphSource source = new GuidedPathVisualGraphSource(
                new GraphNetworkVisualSource(result.network()),
                result.path(),
                viewerLocationSource(playerId),
                guidedPathOptions,
                options.goalMarkerEnabled()
        );
        final Runnable completionCallback = () -> onGuidedPathCompleted(playerId, token, options);
        if (result.backend() == ResolveBackend.BLOCK_DISPLAY) {
            return new GuidedPathCompletionVisualizer(commandContext.loggerFactory(), source,
                    new BlockDisplayGraphRenderer(commandContext.plugin(), playerId),
                    new ProfileGraphDesignResolver(profile), completionCallback);
        }
        return new GuidedPathCompletionVisualizer(commandContext.loggerFactory(), source,
                new ParticleGraphRenderer(commandContext.plugin(), playerId, commandContext.effectExecutor()),
                new ProfileGraphDesignResolver(profile), completionCallback);
    }

    private GuidedPathOptions resolveGuidedPathOptions(final ResolveOptions options) {
        final ConfigurationSection section = commandContext.plugin().getConfig()
                .getConfigurationSection(GUIDED_PATH_CONFIG);
        final GuidedPathOptions configured = GuidedPathOptions.fromConfig(section);
        return new GuidedPathOptions(configured.windowSize(), options.finishRadius(), configured.lookBehind());
    }

    private ViewerLocationSource viewerLocationSource(final UUID playerId) {
        return () -> {
            final Player player = commandContext.plugin().getServer().getPlayer(playerId);
            return player == null ? null : player.getLocation();
        };
    }

    private void onGuidedPathCompleted(final UUID playerId, final long token, final ResolveOptions options) {
        if (!commandContext.sessionManager().markCompleted(playerId, token)) {
            return;
        }
        final Player player = commandContext.plugin().getServer().getPlayer(playerId);
        if (player != null) {
            player.sendMessage("Resolve guidance complete.");
        }
        final long cleanupDelayTicks = options.finishCleanupDelayTicks();
        if (cleanupDelayTicks <= NO_CLEANUP_DELAY_TICKS) {
            commandContext.sessionManager().clearIfCurrent(playerId, token);
            return;
        }
        commandContext.plugin().getServer().getScheduler().runTaskLater(commandContext.plugin(),
                () -> commandContext.sessionManager().clearIfCurrent(playerId, token), cleanupDelayTicks);
    }

    private int cancelOwnGuidance(final CommandContext<CommandSourceStack> context) {
        final CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof final Player player)) {
            commandContext.send(context, "Console must specify a player: /bk resolve cancel <player>.");
            return 0;
        }
        return cancelGuidanceForPlayer(context, player);
    }

    private int cancelGuidance(final CommandContext<CommandSourceStack> context) {
        final Player player = resolveTargetPlayer(context);
        if (player == null) {
            return 0;
        }
        return cancelGuidanceForPlayer(context, player);
    }

    private int cancelGuidanceForPlayer(final CommandContext<CommandSourceStack> context, final Player player) {
        final boolean cancelled = commandContext.sessionManager().cancel(player.getUniqueId());
        if (cancelled) {
            commandContext.send(context, "Cancelled resolve guidance for " + player.getName() + ".");
        } else {
            commandContext.send(context, "No active resolve guidance for " + player.getName() + ".");
        }
        return Command.SINGLE_SUCCESS;
    }

    private Player resolveTargetPlayer(final CommandContext<CommandSourceStack> context) {
        final String playerName = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        final Player player = commandContext.plugin().getServer().getPlayerExact(playerName);
        if (player == null) {
            commandContext.send(context, "Player " + playerName + " is not online.");
        }
        return player;
    }

    @SuppressWarnings("PMD.CommentDefaultAccessModifier")
    private record ResolveResult(Graph graph, GraphNetwork network, PathResult path, ResolveBackend backend,
                                 String message) {

        static ResolveResult graph(final Graph graph, final ResolveBackend backend) {
            return new ResolveResult(graph, null, null, backend, "Showing graph " + graph.getName() + ".");
        }

        static ResolveResult path(final GraphNetwork network, final PathResult path, final ResolveBackend backend,
                                  final String message) {
            return new ResolveResult(null, network, path, backend, message);
        }

        static ResolveResult failure(final String message) {
            return new ResolveResult(null, null, null, null, message);
        }

        boolean success() {
            return graph != null || network != null;
        }
    }
}
