package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Brigadier command registration for graph editor operations.
 */
public class EditorCommand {

    /**
     * Context containing dependencies and helpers for the editor command.
     */
    private final EditorCommandContext commandContext;

    /**
     * Initializes the editor command and registers it with the plugin lifecycle.
     *
     * @param plugin          The JavaPlugin instance.
     * @param loggerFactory   The LoggerFactory instance.
     * @param editorService   The EditorService for managing editor sessions.
     * @param graphRepository The graph repository for graph data operations.
     * @param localization    The localization service.
     */
    public EditorCommand(final JavaPlugin plugin, final LoggerFactory loggerFactory, final EditorService editorService,
                         final GraphRepository graphRepository, final Localization localization) {
        this.commandContext = new EditorCommandContext(plugin, loggerFactory, editorService, graphRepository,
                localization);

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar()
                .register(commandTree(), "Graph editor commands"));
    }

    private LiteralCommandNode<CommandSourceStack> commandTree() {
        final EditorSessionSubcommands sessionSubcommands = new EditorSessionSubcommands(commandContext);
        final EditorPlacementSubcommands placementSubcommands = new EditorPlacementSubcommands(commandContext);
        final EditorSettingsSubcommands settingsSubcommands = new EditorSettingsSubcommands(commandContext);
        final EditorViewSubcommands viewSubcommands = new EditorViewSubcommands(commandContext);
        final EditorSelectionSubcommands selectionSubcommands = new EditorSelectionSubcommands(commandContext);
        final EditorEdgeSubcommands edgeSubcommands = new EditorEdgeSubcommands(commandContext);
        final EditorDeleteSubcommands deleteSubcommands = new EditorDeleteSubcommands(commandContext);
        final EditorWarpSubcommands warpSubcommands = new EditorWarpSubcommands(commandContext);
        return Commands.literal("bkeditor")
                .then(sessionSubcommands.create())
                .then(sessionSubcommands.edit())
                .then(sessionSubcommands.rename())
                .then(sessionSubcommands.finish())
                .then(sessionSubcommands.cancel())
                .then(placementSubcommands.preview())
                .then(placementSubcommands.place())
                .then(placementSubcommands.continuePlacement())
                .then(placementSubcommands.undo())
                .then(settingsSubcommands.settings())
                .then(settingsSubcommands.preset())
                .then(viewSubcommands.view())
                .then(selectionSubcommands.select())
                .then(selectionSubcommands.selection())
                .then(edgeSubcommands.edge())
                .then(deleteSubcommands.delete())
                .then(warpSubcommands.warp())
                .build();
    }
}
