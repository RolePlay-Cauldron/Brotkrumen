package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for guided resolve goal completion effects.
 *
 * @param messageEnabled    whether completion sends a chat message
 * @param soundEnabled      whether completion plays a sound
 * @param soundName         configured Bukkit sound key
 * @param soundVolume       sound volume
 * @param soundPitch        sound pitch
 * @param titleEnabled      whether completion sends title feedback
 * @param titleFadeInTicks  title fade-in duration in ticks
 * @param titleStayTicks    title stay duration in ticks
 * @param titleFadeOutTicks title fade-out duration in ticks
 */
public record ResolveGoalOptions(boolean messageEnabled, boolean soundEnabled, String soundName,
                                 float soundVolume, float soundPitch, boolean titleEnabled,
                                 int titleFadeInTicks, int titleStayTicks, int titleFadeOutTicks) {

    private static final boolean DEFAULT_MESSAGE_ENABLED = true;

    private static final boolean DEFAULT_SOUND_ENABLED = true;

    private static final String DEFAULT_SOUND_NAME = "entity.player.levelup";

    private static final float DEFAULT_SOUND_VOLUME = 1.0F;

    private static final float DEFAULT_SOUND_PITCH = 1.0F;

    private static final boolean DEFAULT_TITLE_ENABLED = false;

    private static final int DEFAULT_TITLE_FADE_IN_TICKS = 10;

    private static final int DEFAULT_TITLE_STAY_TICKS = 40;

    private static final int DEFAULT_TITLE_FADE_OUT_TICKS = 10;

    /**
     * Normalizes invalid values.
     */
    public ResolveGoalOptions {
        if (soundName == null || soundName.isBlank()) {
            soundName = DEFAULT_SOUND_NAME;
        }
        soundVolume = Math.max(0.0F, soundVolume);
        soundPitch = Math.max(0.0F, soundPitch);
        titleFadeInTicks = Math.max(0, titleFadeInTicks);
        titleStayTicks = Math.max(0, titleStayTicks);
        titleFadeOutTicks = Math.max(0, titleFadeOutTicks);
    }

    /**
     * Built-in defaults.
     *
     * @return default options
     */
    public static ResolveGoalOptions defaults() {
        return new ResolveGoalOptions(DEFAULT_MESSAGE_ENABLED, DEFAULT_SOUND_ENABLED, DEFAULT_SOUND_NAME,
                DEFAULT_SOUND_VOLUME, DEFAULT_SOUND_PITCH, DEFAULT_TITLE_ENABLED, DEFAULT_TITLE_FADE_IN_TICKS,
                DEFAULT_TITLE_STAY_TICKS, DEFAULT_TITLE_FADE_OUT_TICKS);
    }

    /**
     * Loads options from config.
     *
     * @param section config section
     * @return loaded options
     */
    public static ResolveGoalOptions fromConfig(final ConfigurationSection section) {
        final ResolveGoalOptions defaults = defaults();
        if (section == null) {
            return defaults;
        }
        final ConfigurationSection messageSection = section.getConfigurationSection("message");
        final ConfigurationSection soundSection = section.getConfigurationSection("sound");
        final ConfigurationSection titleSection = section.getConfigurationSection("title");
        return new ResolveGoalOptions(
                booleanValue(messageSection, "enabled", defaults.messageEnabled()),
                booleanValue(soundSection, "enabled", defaults.soundEnabled()),
                stringValue(soundSection, "name", defaults.soundName()),
                (float) doubleValue(soundSection, "volume", defaults.soundVolume()),
                (float) doubleValue(soundSection, "pitch", defaults.soundPitch()),
                booleanValue(titleSection, "enabled", defaults.titleEnabled()),
                intValue(titleSection, "fadeInTicks", defaults.titleFadeInTicks()),
                intValue(titleSection, "stayTicks", defaults.titleStayTicks()),
                intValue(titleSection, "fadeOutTicks", defaults.titleFadeOutTicks())
        );
    }

    private static boolean booleanValue(final ConfigurationSection section, final String path, final boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    private static String stringValue(final ConfigurationSection section, final String path, final String fallback) {
        return section == null ? fallback : section.getString(path, fallback);
    }

    private static double doubleValue(final ConfigurationSection section, final String path, final double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private static int intValue(final ConfigurationSection section, final String path, final int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }
}
