package com.github.roleplaycauldron.brotkrumen.language;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves raw localized messages loaded from versioned language files.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class Localization {
    private static final String PREFIX_KEY = "prefix";

    private final MiniMessage miniMessage;

    private final LocalizationLoader loader;

    private String configuredDefaultTag;

    private final Map<String, Map<String, String>> messagesByTag;

    private HealthState state;

    /**
     * Creates a localization resolver and immediately loads language files.
     *
     * @param log                  logger
     * @param plugin               plugin
     * @param configuredDefaultTag configured default language tag for non-player contexts
     */
    public Localization(final WrappedLogger log, final Plugin plugin, final String configuredDefaultTag) {
        this.miniMessage = MiniMessage.builder().build();
        this.loader = new LocalizationLoader(Objects.requireNonNull(log, "log"), Objects.requireNonNull(plugin, "plugin"));
        this.configuredDefaultTag = LocalizationTags.normalize(configuredDefaultTag);
        this.messagesByTag = new ConcurrentHashMap<>();
        this.state = HealthState.FAILED;
        reload();
    }

    /**
     * Reloads localization files.
     */
    public final void reload() {
        final LocalizationLoader.LoadSnapshot snapshot = loader.load(configuredDefaultTag);
        this.messagesByTag.clear();
        this.messagesByTag.putAll(snapshot.messagesByTag());
        this.state = snapshot.healthState();
    }

    /**
     * Reloads localization files using a new configured default language tag.
     *
     * @param configuredDefaultTag configured default language tag for non-player contexts
     */
    public final void reload(final String configuredDefaultTag) {
        this.configuredDefaultTag = LocalizationTags.normalize(configuredDefaultTag);
        reload();
    }

    /**
     * Returns the last computed localization health state.
     *
     * @return current localization health state
     */
    public HealthState healthState() {
        return state;
    }

    /**
     * Resolves a raw, unformatted message in non-player context.
     *
     * @param key message key
     * @return stripped message
     */
    public String getRawMessageWithoutFormats(final String key) {
        return miniMessage.stripTags(resolveFromCandidates(nonPlayerCandidateTags(), key));
    }

    /**
     * Resolves a raw, unformatted message for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param key         message key
     * @return stripped message
     */
    public String getRawMessageWithoutFormats(final String languageTag, final String key) {
        return miniMessage.stripTags(resolveFromCandidates(playerCandidateTags(languageTag), key));
    }

    /**
     * Resolves a localized message as an Adventure component in non-player context.
     *
     * @param key message key
     * @return rendered component
     */
    public Component getFormattedMessage(final String key) {
        final String rawMessage = resolveFromCandidates(nonPlayerCandidateTags(), key);
        return deserialize(rawMessage, Map.of());
    }

    /**
     * Resolves a localized message with MiniMessage replacements in non-player context.
     *
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return rendered component
     */
    public Component getFormattedMessage(final String key, final Map<String, String> replacements) {
        final String rawMessage = resolveFromCandidates(nonPlayerCandidateTags(), key);
        return deserialize(rawMessage, replacements);
    }

    /**
     * Resolves a localized message as an Adventure component for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param key         message key
     * @return rendered component
     */
    public Component getFormattedMessage(final String languageTag, final String key) {
        return getFormattedMessage(languageTag, key, Map.of());
    }

    /**
     * Resolves a localized message with MiniMessage replacements for a caller-provided language tag.
     *
     * @param languageTag  tag such as {@code de-de} or {@code de-de-dark}
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return rendered component
     */
    public Component getFormattedMessage(final String languageTag, final String key,
                                         final Map<String, String> replacements) {
        final String rawMessage = resolveFromCandidates(playerCandidateTags(languageTag), key);
        return deserialize(rawMessage, replacements);
    }

    /**
     * Resolves a prefixed localized message in non-player context.
     *
     * @param key message key
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String key) {
        return getPrefixComponent(nonPlayerCandidateTags()).append(getFormattedMessage(key));
    }

    /**
     * Resolves a prefixed localized message with replacements in non-player context.
     *
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String key, final Map<String, String> replacements) {
        return getPrefixComponent(nonPlayerCandidateTags()).append(getFormattedMessage(key, replacements));
    }

    /**
     * Resolves a prefixed localized message for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param key         message key
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String languageTag, final String key) {
        return getPrefixedMessage(languageTag, key, Map.of());
    }

    /**
     * Resolves a prefixed localized message with replacements for a caller-provided language tag.
     *
     * @param languageTag  tag such as {@code de-de} or {@code de-de-dark}
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String languageTag, final String key,
                                        final Map<String, String> replacements) {
        return getPrefixComponent(playerCandidateTags(languageTag))
                .append(getFormattedMessage(languageTag, key, replacements));
    }

    /**
     * Prefixes an existing component in non-player context.
     *
     * @param message message body
     * @return prefixed message
     */
    public Component getPrefixedMessage(final Component message) {
        return getPrefixComponent(nonPlayerCandidateTags()).append(message);
    }

    /**
     * Prefixes an existing component for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param message     message body
     * @return prefixed message
     */
    public Component getPrefixedMessage(final String languageTag, final Component message) {
        return getPrefixComponent(playerCandidateTags(languageTag)).append(message);
    }

    /**
     * Parses an arbitrary MiniMessage string in non-player context.
     *
     * @param message raw MiniMessage input
     * @return rendered component
     */
    public Component getMessageFromString(final String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Prefixes an arbitrary MiniMessage string in non-player context.
     *
     * @param message raw MiniMessage input
     * @return prefixed rendered component
     */
    public Component getPrefixedMessageFromString(final String message) {
        return getPrefixedMessage(getMessageFromString(message));
    }

    /**
     * Prefixes an arbitrary MiniMessage string for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param message     raw MiniMessage input
     * @return prefixed rendered component
     */
    public Component getPrefixedMessageFromString(final String languageTag, final String message) {
        return getPrefixedMessage(languageTag, getMessageFromString(message));
    }

    private Set<String> playerCandidateTags(final String requestedTag) {
        final String normalizedRequested = LocalizationTags.normalize(requestedTag);
        final Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalizedRequested);
        candidates.add(LocalizationTags.languageOnly(normalizedRequested));
        candidates.add(configuredDefaultTag);
        candidates.add(LocalizationTags.languageOnly(configuredDefaultTag));
        candidates.add(LocalizationTags.HARD_FALLBACK_TAG);
        return candidates;
    }

    private Set<String> nonPlayerCandidateTags() {
        final Set<String> candidates = new LinkedHashSet<>();
        candidates.add(configuredDefaultTag);
        candidates.add(LocalizationTags.languageOnly(configuredDefaultTag));
        candidates.add(LocalizationTags.HARD_FALLBACK_TAG);
        return candidates;
    }

    private String resolveFromCandidates(final Set<String> candidates, final String key) {
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            final Map<String, String> localeMessages = messagesByTag.get(candidate);
            if (localeMessages != null && localeMessages.containsKey(key)) {
                return localeMessages.get(key);
            }
        }
        return "No message to the key: '" + key + "' found";
    }

    private Component getPrefixComponent(final Set<String> candidates) {
        final String prefix = resolveFromCandidates(candidates, PREFIX_KEY);
        return miniMessage.deserialize(prefix);
    }

    private Component deserialize(final String rawMessage, final Map<String, String> replacements) {
        final Map<String, String> resolvedReplacements = replacements == null ? Map.of() : replacements;
        if (resolvedReplacements.isEmpty()) {
            return miniMessage.deserialize(rawMessage);
        }
        final TagResolver resolver = TagResolver.resolver(resolvedReplacements.entrySet().stream()
                .map(entry -> Placeholder.parsed(entry.getKey(), entry.getValue()))
                .toList());
        return miniMessage.deserialize(rawMessage, resolver);
    }

    /**
     * Health indicator for runtime localization usage.
     */
    public enum HealthState {
        /**
         * Localization data is fully loaded and complete.
         */
        READY,
        /**
         * Localization loaded but with missing or invalid locale resources.
         */
        DEGRADED,
        /**
         * Localization could not load any usable locale resources.
         */
        FAILED
    }
}
