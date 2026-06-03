package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor deletion subcommands.
 */
public final class EditorDeleteSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private EditorDeleteSubcommands() {
    }

    /**
     * Builds the delete subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the delete subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> delete(final EditorCommandContext commandContext) {
        return Commands.literal("delete")
                .then(Commands.literal("node")
                        .executes(context -> {
                            final org.bukkit.entity.Player player = commandContext.player(context);
                            return player == null
                                    ? EditorCommandFeedback.playerOnly(commandContext, context)
                                    : EditorCommandFeedback.send(commandContext, player,
                                    commandContext.editorService().deleteSelectedNode(player.getUniqueId()));
                        }))
                .then(Commands.literal("graph")
                        .then(Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    final String remaining = builder.getRemainingLowerCase();
                                    commandContext.graphService().getAllGraphs()
                                            .stream()
                                            .map(com.github.roleplaycauldron.brotkrumen.graph.Graph::getName)
                                            .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(remaining))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    final org.bukkit.entity.Player player = commandContext.player(context);
                                    return player == null
                                            ? EditorCommandFeedback.playerOnly(commandContext, context)
                                            : EditorCommandFeedback.send(commandContext, player,
                                            commandContext.editorService().deletePersistedGraph(
                                                    StringArgumentType.getString(context, GRAPH_ARGUMENT)));
                                })));
    }
}

