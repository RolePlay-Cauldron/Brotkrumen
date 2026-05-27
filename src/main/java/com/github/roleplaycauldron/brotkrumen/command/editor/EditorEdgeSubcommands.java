package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor edge mutation subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorEdgeSubcommands {

    private static final String TYPE_ARGUMENT = "type";

    private static final String STATE_ARGUMENT = "state";

    private EditorEdgeSubcommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> edge(final EditorCommandContext commandContext) {
        return Commands.literal("edge")
                .then(Commands.literal("set")
                        .then(edgeTypeArgument().executes(context -> updateEdge(commandContext, context,
                                EdgeAction.SET))))
                .then(Commands.literal("type")
                        .then(edgeTypeArgument().executes(context -> updateEdge(commandContext, context,
                                EdgeAction.TYPE))))
                .then(Commands.literal("state")
                        .then(Commands.argument(STATE_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (final EditorService.EdgeState state : EditorService.EdgeState.values()) {
                                        builder.suggest(state.configValue());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> updateEdgeState(commandContext, context))))
                .then(Commands.literal("remove").executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player,
                                commandContext.editorService().removeSelectedEdge(player.getUniqueId())))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> edgeTypeArgument() {
        return Commands.argument(TYPE_ARGUMENT, StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (final EditorService.EdgeType type : EditorService.EdgeType.values()) {
                        builder.suggest(type.configValue());
                    }
                    return builder.buildFuture();
                });
    }

    private static int updateEdge(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final EdgeAction action) {
        final EditorService.EdgeType edgeType = EditorService.EdgeType.parse(
                StringArgumentType.getString(context, TYPE_ARGUMENT)).orElse(null);
        return withPlayer(commandContext, context, player -> commandContext.send(player, action == EdgeAction.SET
                ? commandContext.editorService().createSelectedNodeEdge(player.getUniqueId(), edgeType)
                : commandContext.editorService().updateSelectedEdgeType(player.getUniqueId(), edgeType)));
    }

    private static int updateEdgeState(final EditorCommandContext commandContext,
                                       final CommandContext<CommandSourceStack> context) {
        final EditorService.EdgeState edgeState = EditorService.EdgeState.parse(
                StringArgumentType.getString(context, STATE_ARGUMENT)).orElse(null);
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().updateSelectedEdgeState(player.getUniqueId(), edgeState)));
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? 0 : action.run(player);
    }

    private enum EdgeAction {
        SET,
        TYPE
    }

    @FunctionalInterface
    private interface PlayerAction {
        int run(Player player);
    }
}
