package com.github.roleplaycauldron.brotkrumen.language;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and validates localization files from plugin storage.
 */
final class LocalizationLoader {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private static final String PREFIX_KEY = "prefix";

    private static final String MESSAGES_PATH = "messages";

    private static final String INVALID_FILE_PREFIX = "Rejected locale file '";

    private final WrappedLogger log;

    private final Plugin plugin;

    /* default */ LocalizationLoader(final WrappedLogger log, final Plugin plugin) {
        this.log = log;
        this.plugin = plugin;
    }

    /* default */ LoadSnapshot load(final String configuredDefaultTag) {
        final File[] files = listLanguageFiles();
        final Map<String, Map<String, String>> loaded = new ConcurrentHashMap<>();
        boolean hadInvalidFiles = false;
        for (final File file : files) {
            final LocaleData data = parseLocaleFile(file);
            if (data == null) {
                hadInvalidFiles = true;
                continue;
            }
            loaded.put(data.tag(), data.messages());
            log.info("Loaded language: " + data.tag() + " (" + data.messages().size() + " messages)");
        }
        final Localization.HealthState state = resolveHealthState(loaded, configuredDefaultTag, hadInvalidFiles);
        log.info("Reloaded messages for " + loaded.size() + " language files (state=" + state + ")");
        return new LoadSnapshot(loaded, state);
    }

    private File[] listLanguageFiles() {
        final File langDir = new File(plugin.getDataFolder(), "language");
        if (!langDir.exists() && !langDir.mkdirs()) {
            log.error("Could not create languages directory.");
            return new File[0];
        }
        final File[] files = langDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            log.error("Could not list language files.");
            return new File[0];
        }
        return files;
    }

    private LocaleData parseLocaleFile(final File file) {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final int schemaVersion = config.getInt("schemaVersion", -1);
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': unsupported schemaVersion=" + schemaVersion);
            return null;
        }

        final String configuredTag = config.getString("locale");
        if (configuredTag == null || configuredTag.isBlank()) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': missing locale field.");
            return null;
        }

        final String prefix = config.getString(PREFIX_KEY);
        if (prefix == null || prefix.isBlank()) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': missing prefix field.");
            return null;
        }

        final ConfigurationSection messagesSection = config.getConfigurationSection(MESSAGES_PATH);
        if (messagesSection == null) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': missing '" + MESSAGES_PATH + "' section.");
            return null;
        }

        final Map<String, String> localeMessages = new ConcurrentHashMap<>();
        localeMessages.put(PREFIX_KEY, prefix);
        for (final Map.Entry<String, Object> entry : messagesSection.getValues(true).entrySet()) {
            if (entry.getValue() instanceof final String value) {
                localeMessages.put(entry.getKey(), value);
            }
        }
        return new LocaleData(LocalizationTags.normalize(configuredTag), localeMessages);
    }

    private Localization.HealthState resolveHealthState(final Map<String, Map<String, String>> loaded,
                                                        final String configuredDefaultTag,
                                                        final boolean hadInvalidFiles) {
        if (loaded.isEmpty()) {
            log.error("Localization failed: no valid language files could be loaded.");
            return Localization.HealthState.FAILED;
        }

        final boolean hasHardFallback = loaded.containsKey(LocalizationTags.HARD_FALLBACK_TAG);
        final boolean hasConfiguredDefault = loaded.containsKey(configuredDefaultTag)
                || loaded.containsKey(LocalizationTags.languageOnly(configuredDefaultTag));
        if (hadInvalidFiles || !hasHardFallback || !hasConfiguredDefault) {
            if (!hasHardFallback) {
                log.error("Localization degraded: required hard fallback language tag en-us is missing.");
            }
            if (!hasConfiguredDefault) {
                log.error("Localization degraded: configured default language tag '" + configuredDefaultTag + "' is missing.");
            }
            return Localization.HealthState.DEGRADED;
        }
        return Localization.HealthState.READY;
    }

    /**
     * Represents a snapshot of loaded localization data, including the set of
     * localized messages organized by language tag and the current health state
     * of the localization system.
     * <p>
     * The snapshot is typically used as a container for transferring the
     * localization data and its associated health state between different
     * components of the localization loader.
     *
     * @param messagesByTag a mapping of language tags to their respective
     *                      localized messages; each language tag corresponds
     *                      to a map of message keys to message strings.
     * @param healthState   the health state of the loaded localization data,
     *                      indicating whether the data is complete, degraded,
     *                      or failed to load.
     */
    /* default */ record LoadSnapshot(Map<String, Map<String, String>> messagesByTag,
                                      Localization.HealthState healthState) {
    }

    /**
     * Represents a container for a specific set of localized messages associated
     * with a particular language tag.
     *
     * @param tag      the language tag for which the messages are intended.
     * @param messages a map of message keys to their corresponding localized
     *                 strings for the specified language tag.
     */
    private record LocaleData(String tag, Map<String, String> messages) {
    }
}
