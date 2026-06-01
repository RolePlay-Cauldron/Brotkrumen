package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds managed warp editor subcommands.
 */
public final class EditorWarpSubcommands {

    private static final String KEY_ARGUMENT = "key";

    private EditorWarpSubcommands() {
    }

    /**
     * Builds the warp management subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the warp subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> warp(final EditorCommandContext commandContext) {
        return Commands.literal("warp")
                .then(create(commandContext, "selected"))
                .then(create(commandContext, "here"))
                .then(Commands.literal("remove").then(key(KEY_ARGUMENT).executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player, commandContext.editorService().removeWarp(
                                player.getUniqueId(), StringArgumentType.getString(context, KEY_ARGUMENT)))))))
                .then(list(commandContext))
                .then(set(commandContext));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> create(final EditorCommandContext commandContext,
                                                                     final String kind) {
        return Commands.literal(kind).then(key(KEY_ARGUMENT).executes(context -> withPlayer(commandContext, context, player -> {
            final String key = StringArgumentType.getString(context, KEY_ARGUMENT);
            return commandContext.send(player, "here".equals(kind)
                    ? commandContext.editorService().createWarpHere(player.getUniqueId(), key, player.getLocation())
                    : commandContext.editorService().createSelectedWarp(player.getUniqueId(), key));
        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> list(final EditorCommandContext commandContext) {
        return Commands.literal("list")
                .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                        commandContext.editorService().listWarps(player.getUniqueId(), false))))
                .then(Commands.literal("all").executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player,
                                commandContext.editorService().listWarps(player.getUniqueId(), true)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> set(final EditorCommandContext commandContext) {
        return Commands.literal("set")
                .then(Commands.literal("cost").then(key(KEY_ARGUMENT).then(Commands.argument("cost",
                        DoubleArgumentType.doubleArg(0.0D)).executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player, commandContext.editorService().updateWarpCost(
                                player.getUniqueId(), StringArgumentType.getString(context, KEY_ARGUMENT),
                                DoubleArgumentType.getDouble(context, "cost"))))))))
                .then(Commands.literal("enabled").then(key(KEY_ARGUMENT).then(Commands.argument("enabled",
                        BoolArgumentType.bool()).executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player, commandContext.editorService().updateWarpEnabled(
                                player.getUniqueId(), StringArgumentType.getString(context, KEY_ARGUMENT),
                                BoolArgumentType.getBool(context, "enabled"))))))))
                .then(Commands.literal("permission").then(key(KEY_ARGUMENT).then(Commands.argument("required",
                        BoolArgumentType.bool()).executes(context -> withPlayer(commandContext, context,
                        player -> commandContext.send(player, commandContext.editorService().updateWarpPermission(
                                player.getUniqueId(), StringArgumentType.getString(context, KEY_ARGUMENT),
                                BoolArgumentType.getBool(context, "required"))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> key(
            final String name) {
        return Commands.argument(name, StringArgumentType.word());
    }

    private static int withPlayer(final EditorCommandContext commandContext,
                                  final CommandContext<CommandSourceStack> context,
                                  final PlayerAction action) {
        final Player player = commandContext.player(context);
        return player == null ? 0 : action.run(player);
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
