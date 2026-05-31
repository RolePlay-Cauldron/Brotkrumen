package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor deletion subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorDeleteSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private EditorDeleteSubcommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> delete(final EditorCommandContext commandContext) {
        return Commands.literal("delete")
                .then(Commands.literal("graph")
                        .then(Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    commandContext.graphService().getAllGraphs()
                                            .forEach(graph -> builder.suggest(graph.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    final org.bukkit.entity.Player player = commandContext.player(context);
                                    return player == null ? 0 : commandContext.send(player,
                                            commandContext.editorService().deletePersistedGraph(
                                                    StringArgumentType.getString(context, GRAPH_ARGUMENT)));
                                })));
    }
}
