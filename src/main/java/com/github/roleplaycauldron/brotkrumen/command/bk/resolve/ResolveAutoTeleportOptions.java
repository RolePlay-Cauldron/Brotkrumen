package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for automatic guided resolve teleports.
 *
 * @param enabled                    whether route auto-teleports are enabled
 * @param delaySeconds               delay before teleport execution
 * @param messageEnabled             whether delayed teleports notify the player
 * @param cooldownSeconds            cooldown after execution or cancellation
 * @param cancelWhenPlayerMovesAway  whether delayed teleports cancel when the player leaves range
 * @param cancelRange                maximum distance from source node before cancellation
 * @param localTeleportEnabled       whether local teleport segments execute automatically
 * @param interGraphTeleportEnabled  whether inter-graph teleport segments execute automatically
 * @param warpEnabled                whether warp segments execute automatically
 * @param startFromWarpWhenNoNearbyNode whether resolve can start from a selected warp when no node is nearby
 */
public record ResolveAutoTeleportOptions(boolean enabled, int delaySeconds, boolean messageEnabled,
                                         int cooldownSeconds, boolean cancelWhenPlayerMovesAway, double cancelRange,
                                         boolean localTeleportEnabled, boolean interGraphTeleportEnabled,
                                         boolean warpEnabled, boolean startFromWarpWhenNoNearbyNode) {

    private static final boolean DEFAULT_ENABLED = true;

    private static final int DEFAULT_DELAY_SECONDS = 0;

    private static final boolean DEFAULT_MESSAGE_ENABLED = true;

    private static final int DEFAULT_COOLDOWN_SECONDS = 3;

    private static final boolean DEFAULT_CANCEL_WHEN_PLAYER_MOVES_AWAY = true;

    private static final double DEFAULT_CANCEL_RANGE = 5.0D;

    private static final boolean DEFAULT_TYPE_ENABLED = true;

    /**
     * Normalizes invalid values.
     */
    public ResolveAutoTeleportOptions {
        delaySeconds = Math.max(0, delaySeconds);
        cooldownSeconds = Math.max(0, cooldownSeconds);
        cancelRange = Math.max(0.0D, cancelRange);
    }

    /**
     * Built-in defaults.
     *
     * @return default options
     */
    public static ResolveAutoTeleportOptions defaults() {
        return new ResolveAutoTeleportOptions(DEFAULT_ENABLED, DEFAULT_DELAY_SECONDS, DEFAULT_MESSAGE_ENABLED,
                DEFAULT_COOLDOWN_SECONDS, DEFAULT_CANCEL_WHEN_PLAYER_MOVES_AWAY, DEFAULT_CANCEL_RANGE,
                DEFAULT_TYPE_ENABLED, DEFAULT_TYPE_ENABLED, DEFAULT_TYPE_ENABLED, DEFAULT_TYPE_ENABLED);
    }

    /**
     * Loads options from config.
     *
     * @param section config section
     * @return loaded options
     */
    public static ResolveAutoTeleportOptions fromConfig(final ConfigurationSection section) {
        final ResolveAutoTeleportOptions defaults = defaults();
        if (section == null) {
            return defaults;
        }
        return new ResolveAutoTeleportOptions(
                section.getBoolean("enabled", defaults.enabled()),
                section.getInt("delaySeconds", defaults.delaySeconds()),
                section.getBoolean("messageEnabled", defaults.messageEnabled()),
                section.getInt("cooldownSeconds", defaults.cooldownSeconds()),
                section.getBoolean("cancelWhenPlayerMovesAway", defaults.cancelWhenPlayerMovesAway()),
                section.getDouble("cancelRange", defaults.cancelRange()),
                section.getBoolean("localTeleportEnabled", defaults.localTeleportEnabled()),
                section.getBoolean("interGraphTeleportEnabled", defaults.interGraphTeleportEnabled()),
                section.getBoolean("warpEnabled", defaults.warpEnabled()),
                section.getBoolean("startFromWarpWhenNoNearbyNode", defaults.startFromWarpWhenNoNearbyNode())
        );
    }

    /**
     * Delay converted to ticks.
     *
     * @return delay ticks
     */
    public long delayTicks() {
        return delaySeconds * 20L;
    }

    /**
     * Cooldown converted to ticks.
     *
     * @return cooldown ticks
     */
    public long cooldownTicks() {
        return cooldownSeconds * 20L;
    }
}
