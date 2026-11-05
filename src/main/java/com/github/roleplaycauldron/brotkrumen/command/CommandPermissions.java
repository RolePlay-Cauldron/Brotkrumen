package com.github.roleplaycauldron.brotkrumen.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.function.Predicate;

/**
 * Permission nodes of the Brotkrumen commands and Brigadier requirement helpers.
 * <p>
 * {@code /bk} subcommands each have their own node mirroring the command path, for example {@code /bk list graph}
 * requires {@code brotkrumen.command.bk.list.graph}. The whole {@code /bkeditor} tree is guarded by the single core
 * node {@code brotkrumen.command.bkeditor}. The nodes are not registered with Bukkit, so they default to operators;
 * everyone else needs them granted by a permission plugin. Wildcards such as {@code brotkrumen.command.bk.*} are
 * resolved by the permission plugin.
 */
public final class CommandPermissions {

    private static final String ROOT = "brotkrumen.command.";

    /**
     * {@code /bk version}.
     */
    public static final String BK_VERSION = ROOT + "bk.version";

    /**
     * {@code /bk list graph}.
     */
    public static final String BK_LIST_GRAPH = ROOT + "bk.list.graph";

    /**
     * {@code /bk list network}.
     */
    public static final String BK_LIST_NETWORK = ROOT + "bk.list.network";

    /**
     * {@code /bk reload}.
     */
    public static final String BK_RELOAD = ROOT + "bk.reload";

    /**
     * {@code /bk resolve <player> <targets>} for the sender's own player.
     */
    public static final String BK_RESOLVE = ROOT + "bk.resolve";

    /**
     * {@code /bk resolve cancel}.
     */
    public static final String BK_RESOLVE_CANCEL = ROOT + "bk.resolve.cancel";

    /**
     * Resolve or cancel guidance for a player other than the sender.
     */
    public static final String BK_RESOLVE_OTHERS = ROOT + "bk.resolve.others";

    /**
     * Core node for the whole {@code /bkeditor} command tree.
     */
    public static final String EDITOR = ROOT + "bkeditor";

    private CommandPermissions() {
    }

    /**
     * Builds a Brigadier requirement for a single permission node.
     *
     * @param node permission node
     * @return requirement predicate
     */
    public static Predicate<CommandSourceStack> requires(final String node) {
        return source -> source.getSender().hasPermission(node);
    }

    /**
     * Builds a Brigadier requirement that is met when the sender holds at least one of the nodes.
     * Used for command groups that only contain subcommands.
     *
     * @param nodes permission nodes
     * @return requirement predicate
     */
    public static Predicate<CommandSourceStack> requiresAny(final String... nodes) {
        return source -> {
            for (final String node : nodes) {
                if (source.getSender().hasPermission(node)) {
                    return true;
                }
            }
            return false;
        };
    }
}
