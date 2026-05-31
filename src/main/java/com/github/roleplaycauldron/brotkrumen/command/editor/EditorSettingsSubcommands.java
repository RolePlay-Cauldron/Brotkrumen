package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Builds editor session settings subcommands.
 */
public final class EditorSettingsSubcommands {

    private EditorSettingsSubcommands() {
    }

    /**
     * Builds the settings subcommand.
     *
     * @param commandContext The editor command context.
     * @return The LiteralArgumentBuilder for the settings subcommand.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> settings(final EditorCommandContext commandContext) {
        return Commands.literal("settings")
                .then(Commands.literal("show")
                        .executes(context -> withPlayer(commandContext, context, player -> commandContext.send(player,
                                commandContext.editorService().settingsSummary(player.getUniqueId())))))
                .then(Commands.literal("node-distance")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1))
                                .executes(context -> updateNodeDistance(commandContext, context))))
                .then(Commands.literal("placement")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (final EditorService.PlacementMode mode : EditorService.PlacementMode.values()) {
                                        builder.suggest(mode.configValue());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> updatePlacement(commandContext, context))))
                .then(Commands.literal("continue-requires-node")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .suggests((context, builder) -> {
                                    builder.suggest("true");
                                    builder.suggest("false");
                                    return builder.buildFuture();
                                })
                                .executes(context -> updateContinueRequiresNode(commandContext, context))))
                .then(Commands.literal("preset")
                        .then(Commands.argument("presetName", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    EditorService.supportedPresets().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> updatePreset(commandContext, context))));
    }

    private static int updateNodeDistance(final EditorCommandContext commandContext,
                                          final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().updateNodeDistance(player.getUniqueId(),
                        IntegerArgumentType.getInteger(context, "blocks"))));
    }

    private static int updatePlacement(final EditorCommandContext commandContext,
                                       final CommandContext<CommandSourceStack> context) {
        final String modeName = StringArgumentType.getString(context, "mode");
        final EditorService.PlacementMode mode = EditorService.PlacementMode.parse(modeName).orElse(null);
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().updatePlacementMode(player.getUniqueId(), mode)));
    }

    private static int updateContinueRequiresNode(final EditorCommandContext commandContext,
                                                  final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().updateContinueRequiresNode(player.getUniqueId(),
                        BoolArgumentType.getBool(context, "enabled"))));
    }

    private static int updatePreset(final EditorCommandContext commandContext,
                                    final CommandContext<CommandSourceStack> context) {
        return withPlayer(commandContext, context, player -> commandContext.send(player,
                commandContext.editorService().updatePreset(player.getUniqueId(),
                        StringArgumentType.getString(context, "presetName"))));
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
