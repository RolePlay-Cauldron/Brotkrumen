package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor lifecycle subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorSessionSubcommands {

    private EditorSessionSubcommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(final EditorCommandContext commandContext) {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> createGraph(commandContext, context)));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> edit(final EditorCommandContext commandContext) {
        return Commands.literal("edit")
                .then(Commands.argument("graphName", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            final String remaining = builder.getRemainingLowerCase();
                            commandContext.graphService().getAllGraphs()
                                    .stream()
                                    .map(com.github.roleplaycauldron.brotkrumen.graph.Graph::getName)
                                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(remaining))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> editGraph(commandContext, context)));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> rename(final EditorCommandContext commandContext) {
        return Commands.literal("rename")
                .then(Commands.argument("newName", StringArgumentType.word())
                        .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                                commandContext.editorService().renameActiveGraph(player.getUniqueId(),
                                        StringArgumentType.getString(context, "newName"))))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> finish(final EditorCommandContext commandContext) {
        return Commands.literal("finish")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().finishRouteCreation(player.getUniqueId()))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> cancel(final EditorCommandContext commandContext) {
        return Commands.literal("cancel")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().cancel(player.getUniqueId()))));
    }

    private static int createGraph(final EditorCommandContext commandContext,
                                   final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().startGraphCreation(player.getUniqueId(),
                        StringArgumentType.getString(context, "name"), commandContext.defaultSettings())));
    }

    private static int editGraph(final EditorCommandContext commandContext,
                                 final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().startGraphEdit(player.getUniqueId(),
                        StringArgumentType.getString(context, "graphName"), commandContext.defaultSettings())));
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
