package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor workspace view subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorViewSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private EditorViewSubcommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> view(final EditorCommandContext commandContext) {
        return Commands.literal("view")
                .then(Commands.literal("add").then(graphArgument(commandContext)
                        .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                                commandContext.editorService().addReferenceGraph(player.getUniqueId(),
                                        StringArgumentType.getString(context, GRAPH_ARGUMENT)))))))
                .then(Commands.literal("remove").then(graphArgument(commandContext)
                        .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                                commandContext.editorService().removeReferenceGraph(player.getUniqueId(),
                                        StringArgumentType.getString(context, GRAPH_ARGUMENT)))))))
                .then(Commands.literal("clear").executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player, commandContext.editorService().clearReferenceGraphs(
                                player.getUniqueId())))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> graphArgument(
            final EditorCommandContext commandContext) {
        return Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                .suggests((context, builder) -> {
                    commandContext.graphService().getAllGraphs().forEach(graph -> builder.suggest(graph.getName()));
                    return builder.buildFuture();
                });
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? 0 : action.run(player);
    }

    @FunctionalInterface
    private interface PlayerAction {
        int run(Player player);
    }
}
