package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds editor session settings subcommands.
 */
public final class EditorSettingsSubcommands {

    private final EditorCommandContext commandContext;

    /**
     * Creates settings subcommand builders.
     *
     * @param commandContext editor command context
     */
    public EditorSettingsSubcommands(final EditorCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Builds the settings subcommand.
     *
     * @return settings subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> settings() {
        return Commands.literal("settings")
                .then(Commands.literal("show")
                        .executes(context -> commandContext.withPlayer(context,
                                player -> commandContext.getFeedback().send(player,
                                        commandContext.editorService().settingsSummary(player.getUniqueId())))))
                .then(Commands.literal("node-distance")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1))
                                .executes(this::updateNodeDistance)))
                .then(Commands.literal("placement")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (final EditorService.PlacementMode mode
                                            : EditorService.PlacementMode.values()) {
                                        builder.suggest(mode.configValue());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::updatePlacement)))
                .then(Commands.literal("continue-requires-node")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .suggests((context, builder) -> {
                                    builder.suggest("true");
                                    builder.suggest("false");
                                    return builder.buildFuture();
                                })
                                .executes(this::updateContinueRequiresNode)))
                .then(Commands.literal("preset")
                        .then(Commands.argument("presetName", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    EditorService.supportedPresets().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(this::updatePreset)));
    }

    private int updateNodeDistance(final CommandContext<CommandSourceStack> context) {
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().updateNodeDistance(player.getUniqueId(),
                        IntegerArgumentType.getInteger(context, "blocks"))));
    }

    private int updatePlacement(final CommandContext<CommandSourceStack> context) {
        final String modeName = StringArgumentType.getString(context, "mode");
        final EditorService.PlacementMode mode = EditorService.PlacementMode.parse(modeName).orElse(null);
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().updatePlacementMode(player.getUniqueId(), mode)));
    }

    private int updateContinueRequiresNode(final CommandContext<CommandSourceStack> context) {
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().updateContinueRequiresNode(player.getUniqueId(),
                        BoolArgumentType.getBool(context, "enabled"))));
    }

    private int updatePreset(final CommandContext<CommandSourceStack> context) {
        return commandContext.withPlayer(context, player -> commandContext.getFeedback().send(player,
                commandContext.editorService().updatePreset(player.getUniqueId(),
                        StringArgumentType.getString(context, "presetName"))));
    }
}
