package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.Brotkrumen;
import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveGuidanceSessionManager;
import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveOptions;
import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveService;
import com.github.roleplaycauldron.brotkrumen.command.bk.resolve.ResolveTargetParser;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import com.github.roleplaycauldron.brotkrumen.storage.repository.WarpRepository;
import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.effect.executor.EffectExecutor;

/**
 * Represents the context of a command executed within the Brotkrumen plugin. This record provides
 * access to various services and utilities required to handle command execution effectively.
 * <p>
 * The context includes references to the Brotkrumen plugin instance and other core services
 * such as graph management, network management, storage, visualization, logging, and effect execution.
 * <p>
 * Components:
 *
 * @param plugin              provides core plugin functionality and access to configurations.
 * @param graphRepository        manages graph-related operations such as CRUD operations on graphs.
 * @param graphNetworkRepository handles operations related to graph networks, including inter-graph connections.
 * @param warpRepository         manages warp-related operations.
 * @param storage             responsible for storage operations, facilitating data persistence.
 * @param visualizerRegistry  manages visualizations and controls visibility updates for graph entities.
 * @param loggerFactory       creates logger instances for logging purposes.
 * @param effectExecutor      executes visual and gameplay effects.
 * @param resolveService      handles resolve operations, including pathfinding and target resolution.
 * @param targetParser        parses and validates resolve target specifications.
 * @param sessionManager      tracks pending and active resolve guidance sessions.
 */
public record BkCommandContext(Brotkrumen plugin, GraphRepository graphRepository,
                               GraphNetworkRepository graphNetworkRepository, WarpRepository warpRepository,
                               Storage storage,
                               VisualizerRegistry visualizerRegistry, LoggerFactory loggerFactory,
                               EffectExecutor effectExecutor, ResolveService resolveService,
                               ResolveTargetParser targetParser, ResolveGuidanceSessionManager sessionManager) {

    /**
     * Resolves configuration options for the `/bk resolve` command.
     *
     * @return a {@link ResolveOptions} instance populated with values derived from the plugin's configuration.
     */
    public ResolveOptions resolveOptions() {
        return ResolveOptions.fromConfig(plugin.getConfig());
    }
}
