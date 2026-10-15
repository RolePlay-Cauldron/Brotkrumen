package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.editor.WandItems;
import com.github.roleplaycauldron.brotkrumen.editor.WandMode;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds the editor wand subcommand.
 */
public final class EditorWandSubcommands {

    private final EditorCommandContext commandContext;

    private final WandItems wandItems;

    /**
     * Creates wand subcommand builders.
     *
     * @param commandContext editor command context
     * @param wandItems      wand item helper
     */
    public EditorWandSubcommands(final EditorCommandContext commandContext, final WandItems wandItems) {
        this.commandContext = commandContext;
        this.wandItems = wandItems;
    }

    /**
     * Builds the wand subcommand.
     *
     * @return wand subcommand
     */
    public LiteralArgumentBuilder<CommandSourceStack> wand() {
        return Commands.literal("wand")
                .executes(context -> commandContext.withPlayer(context, player -> {
                    if (!commandContext.editorService().isEditing(player.getUniqueId())) {
                        return commandContext.getFeedback().send(player,
                                EditorService.EditorResult.failure("commands.bkeditor.common.notEditing"));
                    }
                    final WandItems.GiveResult result = wandItems.give(player, commandContext.editorService()
                            .wandMode(player.getUniqueId()).orElse(WandMode.CRUMB));
                    return commandContext.getFeedback().send(player, result == WandItems.GiveResult.INVENTORY_FULL
                            ? EditorService.EditorResult.failure(result.messageKey())
                            : EditorService.EditorResult.success(result.messageKey()));
                }));
    }
}
