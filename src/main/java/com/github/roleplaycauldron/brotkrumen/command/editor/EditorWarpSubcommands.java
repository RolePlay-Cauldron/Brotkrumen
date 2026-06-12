package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds managed warp editor subcommands.
 */
public final class EditorWarpSubcommands {

    private static final String KEY_ARGUMENT = "key";

    private final EditorCommandContext commandContext;

    /**
     * Creates warp subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorWarpSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the warp management subcommand.
     *
     * @return warp subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> warp() {
        return Commands.literal("warp")
                .then(create("selected"))
                .then(create("here"))
                .then(Commands.literal("remove").then(key(KEY_ARGUMENT)
                        .executes(context -> commandContext.withPlayer(context, player -> {
                            commandContext.editorService().removeWarpAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, KEY_ARGUMENT),
                                    result -> commandContext.getFeedback().send(player, result));
                            return Command.SINGLE_SUCCESS;
                        }))))
                .then(list())
                .then(set());
    }

    private LiteralArgumentBuilder<CommandSourceStack> create(final String kind) {
        return Commands.literal(kind).then(key(KEY_ARGUMENT).executes(context ->
                commandContext.withPlayer(context, player -> {
                    final String key = StringArgumentType.getString(context, KEY_ARGUMENT);
                    return commandContext.getFeedback().send(player, "here".equals(kind)
                            ? commandContext.editorService().createWarpHere(player.getUniqueId(), key,
                            player.getLocation())
                            : commandContext.editorService().createSelectedWarp(player.getUniqueId(), key));
                })));
    }

    private LiteralArgumentBuilder<CommandSourceStack> list() {
        return Commands.literal("list")
                .executes(context -> commandContext.withPlayer(context, player -> {
                    commandContext.runEditorOperationAsync(player, () ->
                            commandContext.editorService().listWarps(player.getUniqueId(), false));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("all").executes(context -> commandContext.withPlayer(context, player -> {
                    commandContext.runEditorOperationAsync(player, () ->
                            commandContext.editorService().listWarps(player.getUniqueId(), true));
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private LiteralArgumentBuilder<CommandSourceStack> set() {
        return Commands.literal("set")
                .then(Commands.literal("cost").then(key(KEY_ARGUMENT).then(Commands.argument("cost",
                        DoubleArgumentType.doubleArg(0.0D)).executes(context -> commandContext.withPlayer(context,
                        player -> {
                            commandContext.editorService().updateWarpCostAsync(player.getUniqueId(),
                                    StringArgumentType.getString(context, KEY_ARGUMENT),
                                    DoubleArgumentType.getDouble(context, "cost"),
                                    result -> commandContext.getFeedback().send(player, result));
                            return Command.SINGLE_SUCCESS;
                        })))))
                .then(Commands.literal("enabled").then(key(KEY_ARGUMENT).then(Commands.argument("enabled",
                        BoolArgumentType.bool()).executes(context -> commandContext.withPlayer(context, player -> {
                    commandContext.editorService().updateWarpEnabledAsync(player.getUniqueId(),
                            StringArgumentType.getString(context, KEY_ARGUMENT),
                            BoolArgumentType.getBool(context, "enabled"),
                            result -> commandContext.getFeedback().send(player, result));
                    return Command.SINGLE_SUCCESS;
                })))))
                .then(Commands.literal("permission").then(key(KEY_ARGUMENT).then(Commands.argument("required",
                        BoolArgumentType.bool()).executes(context -> commandContext.withPlayer(context, player -> {
                    commandContext.editorService().updateWarpPermissionAsync(player.getUniqueId(),
                            StringArgumentType.getString(context, KEY_ARGUMENT),
                            BoolArgumentType.getBool(context, "required"),
                            result -> commandContext.getFeedback().send(player, result));
                    return Command.SINGLE_SUCCESS;
                })))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> key(final String name) {
        return Commands.argument(name, StringArgumentType.word());
    }
}
