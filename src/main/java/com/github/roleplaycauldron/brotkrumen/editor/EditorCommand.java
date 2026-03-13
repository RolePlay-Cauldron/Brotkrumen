package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class EditorCommand extends Command {

    private final EditorService editorService;

    private final GraphService graphService;

    public EditorCommand(EditorService editorService, GraphService graphService) {
        super("bkeditor");
        this.editorService = editorService;
        this.graphService = graphService;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull final CommandSender sender,
                                             @NotNull final String alias,
                                             @NonNull final @NotNull String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("create", "finish");
        }
        return List.of();
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender,
                           @NotNull final String commandLabel,
                           @NonNull final @NotNull String[] args) {
        if (!(sender instanceof final Player player)) {
            return false;
        }

        final String subCmd = args[0];

        if (subCmd.equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage("Please specify a graph name and optionally a node distance");
                return false;
            }

            // ToDo Checken ob der Graph bereits existiert in der DB (oder nem Cache, wenn wir einen machen?)

            final String graphName = args[1];
            final String nodeDistance = args.length >= 3 ? args[2] : null;
            final int nodeDistanceInt = NumberUtils.toInt(nodeDistance, 10);
            editorService.startGraphCreation(player.getUniqueId(), graphName, nodeDistanceInt);
            player.sendMessage("We will trace your steps now and create graph " + graphName + "!");
            return true;
        }

        if (subCmd.equalsIgnoreCase("finish")) {
            editorService.finishRouteCreation(player.getUniqueId());
            player.sendMessage("Route creation finished");
            return true;
        }

        if (subCmd.equalsIgnoreCase("edit")) {
            player.sendMessage("Jibbet noch nicht");
            // ToDo: Mehrere Modes / Tools needed -> Fortführen des Creators übers Laufen,
            //   Anpassen von Edge-Flags, Custom Edges Setzen (maybe mit nem Tool das commands ausführt oder so)
        }
        return false;
    }
}
