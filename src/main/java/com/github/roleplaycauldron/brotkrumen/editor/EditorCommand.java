package com.github.roleplaycauldron.brotkrumen.editor;

import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Brigadier command registration for graph editor operations.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CommentRequired", "PMD.AvoidDuplicateLiterals"})
public class EditorCommand {

    private static final String DEFAULT_NODE_DISTANCE_CONFIG = "editor.defaultNodeDistance";

    private static final int FALLBACK_NODE_DISTANCE = 10;

    private final EditorService editorService;

    private final GraphService graphService;

    private final JavaPlugin plugin;

    public EditorCommand(final JavaPlugin plugin, final EditorService editorService, final GraphService graphService) {
        this.plugin = plugin;
        this.editorService = editorService;
        this.graphService = graphService;

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar()
                .register(commandTree(), "Graph editor commands"));
    }

    private LiteralCommandNode<CommandSourceStack> commandTree() {
        return Commands.literal("bkeditor")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(this::createWithDefaultDistance)
                                .then(Commands.argument("nodeDistance", IntegerArgumentType.integer(1))
                                        .executes(this::createWithCommandDistance))))
                .then(Commands.literal("edit")
                        .then(Commands.argument("graphName", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    graphService.getAllGraphs().forEach(graph -> builder.suggest(graph.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(this::editWithDefaultDistance)
                                .then(Commands.argument("nodeDistance", IntegerArgumentType.integer(1))
                                        .executes(this::editWithCommandDistance))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("newName", StringArgumentType.word())
                                .executes(this::rename)))
                .then(Commands.literal("finish")
                        .executes(this::finish))
                .then(Commands.literal("cancel")
                        .executes(this::cancel))
                .build();
    }

    private int createWithDefaultDistance(final CommandContext<CommandSourceStack> context) {
        return create(context, configuredDefaultNodeDistance());
    }

    private int createWithCommandDistance(final CommandContext<CommandSourceStack> context) {
        return create(context, IntegerArgumentType.getInteger(context, "nodeDistance"));
    }

    private int create(final CommandContext<CommandSourceStack> context, final int nodeDistance) {
        final Player player = player(context);
        if (player == null) {
            return 0;
        }

        return send(player, editorService.startGraphCreation(player.getUniqueId(),
                StringArgumentType.getString(context, "name"), nodeDistance));
    }

    private int editWithDefaultDistance(final CommandContext<CommandSourceStack> context) {
        return edit(context, configuredDefaultNodeDistance());
    }

    private int editWithCommandDistance(final CommandContext<CommandSourceStack> context) {
        return edit(context, IntegerArgumentType.getInteger(context, "nodeDistance"));
    }

    private int edit(final CommandContext<CommandSourceStack> context, final int nodeDistance) {
        final Player player = player(context);
        if (player == null) {
            return 0;
        }

        return send(player, editorService.startGraphEdit(player.getUniqueId(),
                StringArgumentType.getString(context, "graphName"), nodeDistance));
    }

    private int rename(final CommandContext<CommandSourceStack> context) {
        final Player player = player(context);
        if (player == null) {
            return 0;
        }

        return send(player, editorService.renameActiveGraph(player.getUniqueId(),
                StringArgumentType.getString(context, "newName")));
    }

    private int finish(final CommandContext<CommandSourceStack> context) {
        final Player player = player(context);
        if (player == null) {
            return 0;
        }

        return send(player, editorService.finishRouteCreation(player.getUniqueId()));
    }

    private int cancel(final CommandContext<CommandSourceStack> context) {
        final Player player = player(context);
        if (player == null) {
            return 0;
        }

        return send(player, editorService.cancel(player.getUniqueId()));
    }

    private Player player(final CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof final Player player) {
            return player;
        }
        context.getSource().getSender().sendMessage("Only players can use the graph editor.");
        return null;
    }

    private int send(final Player player, final EditorService.EditorResult result) {
        player.sendMessage(result.message());
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private int configuredDefaultNodeDistance() {
        return Math.max(1, plugin.getConfig().getInt(DEFAULT_NODE_DISTANCE_CONFIG, FALLBACK_NODE_DISTANCE));
    }
}
