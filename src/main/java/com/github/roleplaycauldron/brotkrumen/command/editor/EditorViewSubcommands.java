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
public final class EditorViewSubcommands {

    private static final String GRAPH_ARGUMENT = "graphName";

    private EditorViewSubcommands() {
    }

    /**
     * Builds the view subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the view subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> view(final EditorCommandContext commandContext) {
        return Commands.literal("view")
                .then(Commands.literal("add").then(graphArgument(commandContext)
                        .executes(context -> withPlayer(commandContext, context, player -> EditorCommandFeedback.send(commandContext, player,
                                commandContext.editorService().addReferenceGraph(player.getUniqueId(),
                                        StringArgumentType.getString(context, GRAPH_ARGUMENT)))))))
                .then(Commands.literal("remove").then(graphArgument(commandContext)
                        .executes(context -> withPlayer(commandContext, context, player -> EditorCommandFeedback.send(commandContext, player,
                                commandContext.editorService().removeReferenceGraph(player.getUniqueId(),
                                        StringArgumentType.getString(context, GRAPH_ARGUMENT)))))))
                .then(Commands.literal("clear").executes(context -> withPlayer(commandContext, context,
                        player -> EditorCommandFeedback.send(commandContext, player, commandContext.editorService().clearReferenceGraphs(
                                player.getUniqueId())))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> graphArgument(
            final EditorCommandContext commandContext) {
        return Commands.argument(GRAPH_ARGUMENT, StringArgumentType.word())
                .suggests((context, builder) -> {
                    final String remaining = builder.getRemainingLowerCase();
                    commandContext.graphService().getAllGraphs().stream()
                            .map(com.github.roleplaycauldron.brotkrumen.graph.Graph::getName)
                            .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(remaining))
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                });
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? EditorCommandFeedback.playerOnly(commandContext, context) : action.run(player);
    }

    /**
     * Functional interface for actions that require a player.
     */
    @FunctionalInterface
    private interface PlayerAction {
        /**
         * Executes the action for the given player.
         *
         * @param player The player.
         * @return The result of the action.
         */
        int run(Player player);
    }
}


