package com.github.roleplaycauldron.brotkrumen.language;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Localization class is used to load and manage messages from multiple {@link YamlConfiguration} files.
 * It uses {@link MiniMessage} to format the messages.
 */
public class Localization {
    /**
     * The prefix for all messages.
     */
    private static final String PREFIX_STRING = "<dark_gray>[<dark_green>Brotkrumen<dark_gray>] <gray>";

    /**
     * The default locale.
     */
    private static final Locale DEFAULT_LOCALE = Locale.US; // ToDo load default from config

    /**
     * The deserialized prefix for all messages.
     */
    private final Component prefix;

    /**
     * The {@link WrappedLogger} for this class.
     */
    private final WrappedLogger log;

    /**
     * The {@link Plugin} used for sync scheduling in {@link #reload()}.
     */
    private final Plugin plugin;

    /**
     * The {@link MiniMessage} for this class.
     */
    private final MiniMessage miniMessage;

    /**
     * The messages loaded from the {@link YamlConfiguration} files, mapped by locale.
     */
    private final Map<Locale, Map<String, String>> messages;

    /**
     * The constructor for the Localization class.
     *
     * @param log    The {@link WrappedLogger} for this class.
     * @param plugin The {@link Plugin} used for sync task scheduling.
     */
    public Localization(final WrappedLogger log, final Plugin plugin) {
        this.log = log;
        this.plugin = plugin;
        this.miniMessage = MiniMessage.builder().build();
        this.prefix = miniMessage.deserialize(PREFIX_STRING);
        this.messages = new ConcurrentHashMap<>();
        loadMessages();
    }

    /**
     * Reloads the messages from the {@link YamlConfiguration} files.
     */
    public void reload() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            loadMessages();
            log.info("Reloaded Messages for " + messages.size() + "language");
        });
    }

    /**
     * Loads the messages from the {@link YamlConfiguration} files in the languages directory.
     */
    private void loadMessages() {
        // ToDo Only load currently needed message-Files and not every message file available
        // ToDo for per player purposes store the current selected language and preload every in the db (if per Player is really neded)
        // ToDo If a language is not preloaded, load it on demand if it exists and is chosen.
        // ToDo if per Player is not necessary, idk what then. This could be a spellbook integration, but we would force them to use MiniMessage.
        final File langDir = new File(plugin.getDataFolder(), "language");
        if (!langDir.exists()) {
            if (langDir.mkdirs()) {
                log.info("Created languages directory.");
            } else {
                log.error("Could not create languages directory.");
                return;
            }
        }

        final File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        messages.clear();
        for (final File file : files) {
            final String name = file.getName().replace(".yml", "");
            final Locale locale = Locale.forLanguageTag(name.replace("_", "-"));
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            final Map<String, Object> loadedMessages = config.getValues(true);
            final Map<String, String> localeMessages = new ConcurrentHashMap<>();

            for (final Map.Entry<String, Object> entry : loadedMessages.entrySet()) {
                if (entry.getValue() instanceof final String stringValue) {
                    localeMessages.put(entry.getKey(), stringValue);
                }
            }
            messages.put(locale, localeMessages);
            log.info("Loaded language: " + locale.toLanguageTag() + " (" + localeMessages.size() + " messages)");
        }
    }

    /**
     * Returns the raw message to the key for the default locale.
     *
     * @param key The key of the message.
     * @return The raw message.
     */
    public String getRawMessageWithoutFormats(final String key) {
        return getRawMessageWithoutFormats(DEFAULT_LOCALE, key);
    }

    /**
     * Returns the raw message to the key for the specified locale.
     *
     * @param locale The locale to use.
     * @param key    The key of the message.
     * @return The raw message.
     */
    public String getRawMessageWithoutFormats(final Locale locale, final String key) {
        final String message = getRawString(locale, key);
        return miniMessage.stripTags(message);
    }

    /**
     * Returns the raw message to the key with formats for the default locale.
     *
     * @param key          The key of the message.
     * @param replacements a {@link Map} with the replacements.
     * @return The raw message.
     */
    public String getRawMessageWithFormats(final String key, final Map<String, String> replacements) {
        return getRawMessageWithFormats(DEFAULT_LOCALE, key, replacements);
    }

    /**
     * Returns the raw message to the key with formats for the specified locale.
     *
     * @param locale       The locale to use.
     * @param key          The key of the message.
     * @param replacements a {@link Map} with the replacements.
     * @return The raw message.
     */
    public String getRawMessageWithFormats(final Locale locale, final String key, final Map<String, String> replacements) {
        return miniMessage.serialize(getFormattedMessage(locale, key, replacements));
    }

    /**
     * Returns the formatted message to the key for the specified player's locale.
     *
     * @param player The player to get the locale from.
     * @param key    The key of the message.
     * @return The formatted message.
     */
    public Component getFormattedMessage(final Player player, final String key) {
        return getFormattedMessage(player.locale(), key, Map.of());
    }

    /**
     * Returns the formatted message to the key with replacements for the specified player's locale.
     *
     * @param player       The player to get the locale from.
     * @param key          The key of the message.
     * @param replacements a {@link Map} with the replacements.
     * @return The formatted message.
     */
    public Component getFormattedMessage(final Player player, final String key, final Map<String, String> replacements) {
        return getFormattedMessage(player.locale(), key, replacements);
    }

    /**
     * Returns the formatted message to the key for the default locale.
     *
     * @param key The key of the message.
     * @return The formatted message.
     */
    public Component getFormattedMessage(final String key) {
        return getFormattedMessage(DEFAULT_LOCALE, key, Map.of());
    }

    /**
     * Returns the formatted message to the key for the specified locale.
     *
     * @param locale The locale to use.
     * @param key    The key of the message.
     * @return The formatted message.
     */
    public Component getFormattedMessage(final Locale locale, final String key) {
        return getFormattedMessage(locale, key, Map.of());
    }

    /**
     * Returns the formatted message to the key with replacements for the specified locale.
     *
     * @param locale       The locale to use.
     * @param key          The key of the message.
     * @param replacements a {@link Map} with the replacements.
     * @return The formatted message.
     */
    public Component getFormattedMessage(final Locale locale, final String key, final Map<String, String> replacements) {
        final String message = getRawString(locale, key);
        if (replacements.isEmpty()) {
            return miniMessage.deserialize(message);
        }
        final TagResolver resolver = TagResolver.resolver(
                replacements.entrySet().stream()
                        .map(entry -> Placeholder.parsed(entry.getKey(), entry.getValue()))
                        .toList()
        );
        return miniMessage.deserialize(message, resolver);
    }

    /**
     * Returns the prefixed message to the key for the specified player's locale.
     *
     * @param player The player to get the locale from.
     * @param key    The key of the message.
     * @return The prefixed message.
     */
    public Component getPrefixedMessage(final Player player, final String key) {
        return prefix.append(getFormattedMessage(player.locale(), key, Map.of()));
    }

    /**
     * Returns the prefixed message to the key with replacements for the specified player's locale.
     *
     * @param player       The player to get the locale from.
     * @param key          The key of the message.
     * @param replacements a {@link Map} with the replacements.
     * @return The prefixed message.
     */
    public Component getPrefixedMessage(final Player player, final String key, final Map<String, String> replacements) {
        return prefix.append(getFormattedMessage(player.locale(), key, replacements));
    }

    /**
     * Returns the prefixed message to the key for the default locale.
     *
     * @param key The key of the message.
     * @return The prefixed message.
     */
    public Component getPrefixedMessage(final String key) {
        return prefix.append(getFormattedMessage(DEFAULT_LOCALE, key, Map.of()));
    }

    /**
     * Returns the prefixed message to the key with replacements for the specified locale.
     *
     * @param locale       The locale to use.
     * @param key          The key of the message.
     * @param replacements a {@link Map} with the replacements.
     * @return The prefixed message.
     */
    public Component getPrefixedMessage(final Locale locale, final String key, final Map<String, String> replacements) {
        return prefix.append(getFormattedMessage(locale, key, replacements));
    }

    /**
     * Returns the prefixed {@link Component} from a {@link String}.
     *
     * @param message The {@link String} to convert.
     * @return The prefixed message from the {@link String}.
     */
    public Component getPrefixedMessageFromString(final String message) {
        return prefix.append(miniMessage.deserialize(message));
    }

    /**
     * Returns the {@link Component} from a {@link String}.
     *
     * @param message The {@link String} to convert.
     * @return The {@link Component} from the {@link String}.
     */
    public Component getMessageFromString(final String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Internal method to get the raw string for a locale with fallback.
     *
     * @param locale The locale.
     * @param key    The key.
     * @return The raw string.
     */
    private String getRawString(final Locale locale, final String key) {
        final Map<String, String> localeMessages = messages.get(locale);
        if (localeMessages != null && localeMessages.containsKey(key)) {
            return localeMessages.get(key);
        }

        // Fallback to default locale
        final Map<String, String> defaultMessages = messages.get(DEFAULT_LOCALE);
        if (defaultMessages != null && defaultMessages.containsKey(key)) {
            return defaultMessages.get(key);
        }

        return "No message to the key: '" + key + "' found";
    }
}
