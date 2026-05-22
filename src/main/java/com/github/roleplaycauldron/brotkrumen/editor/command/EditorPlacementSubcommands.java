package com.github.roleplaycauldron.brotkrumen.editor.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor placement control subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class EditorPlacementSubcommands {

    private EditorPlacementSubcommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> preview(final EditorCommandContext commandContext) {
        return Commands.literal("preview")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().preview(player.getUniqueId()))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> place(final EditorCommandContext commandContext) {
        return Commands.literal("place")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().placeNode(player.getUniqueId(), player.getLocation()))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> continuePlacement(
            final EditorCommandContext commandContext) {
        return Commands.literal("continue")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().continuePlacement(player.getUniqueId()))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> undo(final EditorCommandContext commandContext) {
        return Commands.literal("undo")
                .executes(context -> undo(commandContext, context, 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> undo(commandContext, context,
                                IntegerArgumentType.getInteger(context, "amount"))));
    }

    private static int undo(final EditorCommandContext commandContext,
                            final CommandContext<CommandSourceStack> context,
                            final int amount) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().undo(player.getUniqueId(), amount)));
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
