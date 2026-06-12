package com.github.roleplaycauldron.brotkrumen.command.editor;

import com.github.roleplaycauldron.brotkrumen.editor.EditorService;
import com.github.roleplaycauldron.brotkrumen.language.Localization;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

/**
 * Shared feedback helpers for editor subcommands.
 */
public class EditorCommandFeedback {

    private final Localization localization;

    /**
     * Constructs a new instance of {@link EditorCommandFeedback}.
     *
     * @param localization the localization instance used for generating localized messages within editor commands
     */
    public EditorCommandFeedback(final Localization localization) {
        this.localization = localization;
    }

    /**
     * Sends a player-only error message.
     *
     * @param context command context
     * @return command failure result
     */
    public int playerOnly(final CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage(localization.getPrefixedMessage("commands.common.playerOnlyEditor"));
        return 0;
    }

    /**
     * Sends an editor result message to a player.
     *
     * @param player target player
     * @param result editor result
     * @return command result status
     */
    public int send(final Player player, final EditorService.EditorResult result) {
        if (result.message() != null && !result.message().isBlank()) {
            player.sendMessage(localization.getPrefixedMessage(result.message(), result.replacements()));
        }
        if (result.component() != null) {
            player.sendMessage(localization.getPrefixedMessage(result.component()));
        }
        for (final EditorService.LocalizedMessage message : result.extraMessages()) {
            player.sendMessage(localization.getPrefixedMessage(message.key(), message.renderedReplacements(localization)));
        }
        if (result.actionBarMessage() != null && !result.actionBarMessage().isBlank()) {
            player.sendActionBar(localization.getFormattedMessage(result.actionBarMessage(), result.replacements()));
        }
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }
}
