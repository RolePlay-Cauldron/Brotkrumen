package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor edge mutation subcommands.
 */
public final class EditorEdgeSubcommands {

    private static final String TYPE_ARGUMENT = "type";

    private static final String STATE_ARGUMENT = "state";

    private static final String TRAVERSAL_ARGUMENT = "traversal";

    private final EditorCommandContext commandContext;

    /**
     * Creates edge subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorEdgeSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the edge mutation subcommand.
     *
     * @return edge subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> edge() {
        return Commands.literal("edge")
                .then(Commands.literal("connect").then(edgeTypeArgument().executes(this::connectEdge)))
                .then(Commands.literal("traversal")
                        .then(Commands.argument(TRAVERSAL_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (final EditorService.EdgeTraversal traversal
                                            : EditorService.EdgeTraversal.values()) {
                                        builder.suggest(traversal.configValue());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::updateEdgeTraversal)))
                .then(Commands.literal("state")
                        .then(Commands.argument(STATE_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (final EditorService.EdgeState state : EditorService.EdgeState.values()) {
                                        builder.suggest(state.configValue());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::updateEdgeState)))
                .then(Commands.literal("remove").executes(context -> commandContext.withPlayer(context,
                        player -> commandContext.getFeedback().send(player,
                                commandContext.editorService().removeSelectedEdge(player.getUniqueId())))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> edgeTypeArgument() {
        return Commands.argument(TYPE_ARGUMENT, StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (final EditorService.EdgeType type : EditorService.EdgeType.values()) {
                        builder.suggest(type.configValue());
                    }
                    return builder.buildFuture();
                });
    }

    private int connectEdge(final CommandContext<CommandSourceStack> context) {
        final EditorService.EdgeType edgeType = EditorService.EdgeType.parse(
                StringArgumentType.getString(context, TYPE_ARGUMENT)).orElse(null);
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().createSelectedNodeEdge(player.getUniqueId(), edgeType)));
    }

    private int updateEdgeTraversal(final CommandContext<CommandSourceStack> context) {
        final EditorService.EdgeTraversal traversal = EditorService.EdgeTraversal.parse(
                StringArgumentType.getString(context, TRAVERSAL_ARGUMENT)).orElse(null);
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().updateSelectedEdgeTraversal(player.getUniqueId(), traversal)));
    }

    private int updateEdgeState(final CommandContext<CommandSourceStack> context) {
        final EditorService.EdgeState edgeState = EditorService.EdgeState.parse(
                StringArgumentType.getString(context, STATE_ARGUMENT)).orElse(null);
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().updateSelectedEdgeState(player.getUniqueId(), edgeState)));
    }
}
