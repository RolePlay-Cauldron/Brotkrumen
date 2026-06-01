package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/**
 * Brigadier command registration for public Brotkrumen operations.
 */
public class BkCommand {

    private final BkCommandContext commandContext;

    private final Localization localization;

    /**
     * Constructs a BkCommand instance, initializing the runtime command framework for the Brotkrumen plugin.
     * This class handles the registration and management of Brigadier commands related to public Brotkrumen operations.
     *
     * @param plugin              the Brotkrumen plugin instance responsible for managing the lifecycle and configuration
     * @param graphService        the service used for managing graph-related operations, such as CRUD operations on graphs
     * @param graphNetworkService the service used for handling inter-graph connectivity and network operations
     * @param storage             the storage backend used for persisting data and retrieving stored information
     * @param visualizerRegistry  the registry managing visualizers for rendering or displaying graphical representations
     * @param loggerFactory       the logger factory responsible for creating and managing loggers for various components
     * @param effectExecutor      the executor for managing and executing visual or gameplay effects
     * @param localization        localization service for sender feedback rendering
     */
    public BkCommand(final Brotkrumen plugin, final GraphService graphService,
                     final GraphNetworkService graphNetworkService, final Storage storage,
                     final VisualizerRegistry visualizerRegistry, final LoggerFactory loggerFactory,
                     final EffectExecutor effectExecutor, final Localization localization) {
        this.commandContext = new BkCommandContext(plugin, graphService, graphNetworkService, storage,
                visualizerRegistry, loggerFactory, effectExecutor, new ResolveService(graphService, graphNetworkService),
                new ResolveTargetParser(), new ResolveGuidanceSessionManager(visualizerRegistry));
        this.localization = localization;

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar()
                .register(commandTree(), "Brotkrumen runtime commands"));
    }

    private LiteralCommandNode<CommandSourceStack> commandTree() {
        return Commands.literal("bk")
                .then(new BkVersionSubcommand(commandContext, localization).version())
                .then(new BkListSubcommands(commandContext, localization).list())
                .then(new BkResolveSubcommand(commandContext, localization).resolve())
                .build();
    }
}
